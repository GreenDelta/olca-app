package org.openlca.app.wizards.calculation;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
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
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.ProductSystemDao;
import org.openlca.core.math.CalculationType;
import org.openlca.core.math.SystemCalculator;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.results.SimpleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wizard for setting calculation properties and run the calculation of a
 * product system
 */
public class CalculationWizard extends Wizard {

	private final Setup setup;
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	public CalculationWizard(ProductSystem system) {
		this.setup = Setup.init(system);
		setNeedsProgressMonitor(true);
		setWindowTitle(M.CalculationProperties);
	}

	public static void open(ProductSystem system) {
		if (system == null)
			return;
		boolean doContinue = checkForUnsavedContent();
		if (!doContinue)
			return;
		var wizard = new CalculationWizard(system);
		var dialog = new WizardDialog(UI.shell(), wizard);
		dialog.open();
	}

	private static boolean checkForUnsavedContent() {
		var page = Editors.getActivePage();
		if (page == null)
			return true;
		var ok = EnumSet.of(
			ModelType.PROJECT,
			ModelType.ACTOR,
			ModelType.SOURCE);
		var dirtyEditors = Arrays.stream(page.getDirtyEditors())
			.filter(editor -> {
				var inp = editor.getEditorInput();
				if (!(inp instanceof ModelEditorInput))
					return false;
				var type = ((ModelEditorInput) inp).getDescriptor().type;
				return !ok.contains(type);
			})
			.collect(Collectors.toList());

		if (dirtyEditors.isEmpty())
			return true;
		int answer = Question.askWithCancel(
			M.UnsavedChanges, M.SomeElementsAreNotSaved);
		if (answer == IDialogConstants.NO_ID)
			return true;
		if (answer == IDialogConstants.CANCEL_ID)
			return false;
		for (IEditorPart part : dirtyEditors) {
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
		var ok = new boolean[]{false};
		setup.savePreferences();
		try {
			getContainer().run(true, true, monitor -> {
				monitor.beginTask(M.RunCalculation, IProgressMonitor.UNKNOWN);
				try {
					runCalculation();
					ok[0] = true;
				} catch (OutOfMemoryError err) {
					MemoryError.show();
				} catch (MathIllegalArgumentException e) {
					MsgBox.error("Matrix error", e.getMessage());
				} finally {
					monitor.done();
				}
			});
		} catch (Exception e) {
			ok[0] = false;
			ErrorReporter.on("Calculation failed", e);
		}
		return ok[0];
	}

	private void runCalculation() {
		// for MC simulations, just open the simulation editor
		if (setup.calcType == CalculationType.MONTE_CARLO_SIMULATION) {
			setup.calcSetup.withUncertainties = true;
			SimulationEditor.open(setup.calcSetup);
			return;
		}

		setup.calcSetup.withUncertainties = false;
		boolean upstream = setup.calcType == CalculationType.UPSTREAM_ANALYSIS;

		// run the calculation
		log.trace("run calculation");
		var calc = new SystemCalculator(Database.get());
		var result = upstream
			? calc.calculateFull(setup.calcSetup)
			: calc.calculateContributions(setup.calcSetup);

		// check storage and DQ calculation
		if (setup.storeInventory) {
			log.trace("store inventory");
			saveInventory(result);
		}
		DQResult dqResult = null;
		if (setup.withDataQuality) {
			log.trace("calculate data quality result");
			dqResult = DQResult.of(
				Database.get(), setup.dqSetup, result);
		}

		// sort and open the editor
		log.trace("sort result items");
		Sort.sort(result);
		log.trace("calculation done; open editor");
		ResultEditor.open(setup.calcSetup, result, dqResult);
	}

	private void saveInventory(SimpleResult r) {
		var system = setup.calcSetup.productSystem;
		system.inventory.clear();
		var db = Database.get();
		var sysDao = new ProductSystemDao(db);
		var enviIndex = r.enviIndex();
		if (enviIndex == null || enviIndex.isEmpty()) {
			sysDao.update(system);
			return;
		}

		// load the used flows
		Set<Long> flowIDs = new HashSet<>();
		enviIndex.each((i, f) -> {
			if (f.flow() == null)
				return;
			flowIDs.add(f.flow().id);
		});
		Map<Long, Flow> flows = new FlowDao(db)
			.getForIds(flowIDs).stream()
			.collect(Collectors.toMap(f -> f.id, f -> f));

		// create the exchanges
		enviIndex.each((i, f) -> {
			if (f.flow() == null)
				return;
			Flow flow = flows.get(f.flow().id);
			if (flow == null)
				return;
			var e = Exchange.of(flow);
			e.amount = r.getTotalFlowResult(f);
			e.isInput = f.isInput();
			system.inventory.add(e);
		});

		sysDao.update(system);
	}
}
