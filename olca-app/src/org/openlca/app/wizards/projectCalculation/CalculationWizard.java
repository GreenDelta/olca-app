package org.openlca.app.wizards.projectCalculation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IEditorPart;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.ModelEditorInput;
import org.openlca.app.results.ResultEditor;
import org.openlca.app.results.Sort;
import org.openlca.app.results.simulation.SimulationEditor;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ProductSystemDao;
import org.openlca.core.math.CalculationType;
import org.openlca.core.math.SystemCalculator;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Project;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.results.ContributionResult;
import org.openlca.core.results.SimpleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wizard for setting calculation properties and run the calculation of a
 * product system
 */
public class CalculationWizard extends Wizard {
	Project project;
	List<ProjectVariant> variants;
	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private Setup setup;

	public CalculationWizard(Project project, List<ProjectVariant> variants) {
		this.setup = Setup.init(variants.get(0).productSystem);
		this.project = project;
		this.variants = variants;
		setNeedsProgressMonitor(true);
		setWindowTitle(M.CalculationProperties);
	}

	public static void open(Project project, List<ProjectVariant> variants) {
		if (project == null)
			return;
//		boolean doContinue = checkForUnsavedContent(project);
//		if (!doContinue)
//			return;
		var wizard = new CalculationWizard(project, variants);
		var dialog = new WizardDialog(UI.shell(), wizard);
		dialog.open();
	}

	private static boolean checkForUnsavedContent(Project project) {
		IEditorPart[] dirty = Editors.getActivePage().getDirtyEditors();
		if (dirty.length == 0)
			return true;
		List<IEditorPart> relevant = new ArrayList<>();
		for (IEditorPart part : dirty) {
			if (!(part.getEditorInput() instanceof ModelEditorInput))
				continue;
			ModelEditorInput input = (ModelEditorInput) part.getEditorInput();
			ModelType type = input.getDescriptor().type;
			if (type == ModelType.PROJECT || type == ModelType.ACTOR || type == ModelType.SOURCE)
				continue;
			if (type == ModelType.PRODUCT_SYSTEM && input.getDescriptor().id != project.id)
				continue;
			relevant.add(part);
		}
		if (relevant.isEmpty())
			return true;
		int answer = Question.askWithCancel(M.UnsavedChanges, M.SomeElementsAreNotSaved);
		if (answer == IDialogConstants.NO_ID)
			return true;
		if (answer == IDialogConstants.CANCEL_ID)
			return false;
		for (IEditorPart part : relevant) {
			Editors.getActivePage().saveEditor(part, false);
		}
		return true;
	}

	@Override
	public void addPages() {
		addPage(new CalculationWizardPage(setup));
		addPage(new DQSettingsPage(setup));
	}

	@Override
	public boolean performFinish() {
		setup.savePreferences();
		try {
			var calculation = new Calculation();
			getContainer().run(true, true, calculation);
			if (calculation.outOfMemory)
				MemoryError.show();
			return !calculation.outOfMemory;
		} catch (Exception e) {
			log.error("Calculation failed", e);
			return false;
		}
	}

	private class Calculation implements IRunnableWithProgress {

		private boolean outOfMemory;

		@Override
		public void run(IProgressMonitor monitor) {

			outOfMemory = false;
			monitor.beginTask(M.RunCalculation, IProgressMonitor.UNKNOWN);
			var results = new ArrayList<ContributionResult>();
			try {

				// run the calculation
				log.trace("run calculation");
				var productSystems = project.variants;
				DQResult dqResult = null;
				for (ProjectVariant projectVariant : productSystems) {
					setup = Setup.init(projectVariant.productSystem);

					// for MC simulations, just open the simulation editor
					if (setup.calcType == CalculationType.MONTE_CARLO_SIMULATION) {
						setup.calcSetup.withUncertainties = true;
						SimulationEditor.open(setup.calcSetup);
						return;
					}

					setup.calcSetup.withUncertainties = false;
					boolean upstream = setup.calcType == CalculationType.UPSTREAM_ANALYSIS;
					var calc = new SystemCalculator(Database.get());
					var result = upstream ? calc.calculateFull(setup.calcSetup)
							: calc.calculateContributions(setup.calcSetup);

					// check storage and DQ calculation
					if (setup.storeInventory) {
						log.trace("store inventory");
						saveInventory(result);
					}

					if (setup.withDataQuality) {
						log.trace("calculate data quality result");
						dqResult = DQResult.of(Database.get(), setup.dqSetup, result);
					}
					// sort and open the editor
					log.trace("sort result items");
					Sort.sort(result);
					results.add(result);
				}

				log.trace("calculation done; open editor");
				ResultEditor.open(setup.calcSetup, results, dqResult,variants);
			} catch (OutOfMemoryError e) {
				outOfMemory = true;
			}
			monitor.done();
		}

		private void saveInventory(SimpleResult r) {
			ProductSystem system = setup.calcSetup.productSystem;
			system.inventory.clear();
			IDatabase db = Database.get();
			var sysDao = new ProductSystemDao(db);
			var flowIndex = r.flowIndex();
			if (flowIndex == null || flowIndex.isEmpty()) {
				sysDao.update(system);
				return;
			}

			// load the used flows
			Set<Long> flowIDs = new HashSet<>();
			flowIndex.each((i, f) -> {
				if (f.flow == null)
					return;
				flowIDs.add(f.flow.id);
			});
			Map<Long, Flow> flows = new FlowDao(db).getForIds(flowIDs).stream()
					.collect(Collectors.toMap(f -> f.id, f -> f));

			// create the exchanges
			flowIndex.each((i, f) -> {
				if (f.flow == null)
					return;
				Flow flow = flows.get(f.flow.id);
				if (flow == null)
					return;
				var e = Exchange.of(flow);
				e.amount = r.getTotalFlowResult(f);
				e.isInput = f.isInput;
				system.inventory.add(e);
			});

			sysDao.update(system);
		}
	}
}
