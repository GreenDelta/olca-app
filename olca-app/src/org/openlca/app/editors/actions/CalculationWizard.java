package org.openlca.app.editors.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.inventory.InventoryResultEditor;
import org.openlca.app.inventory.InventoryResultInput;
import org.openlca.app.simulation.SimulationInit;
import org.openlca.app.util.Editors;
import org.openlca.core.editors.analyze.AnalyzeEditor;
import org.openlca.core.editors.analyze.AnalyzeEditorInput;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.SystemCalculator;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.results.AnalysisResult;
import org.openlca.core.results.InventoryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wizard for setting calculation properties and run the calculation of a
 * product system
 */
class CalculationWizard extends Wizard {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private CalculationWizardPage calculationPage;
	private ProductSystem productSystem;

	public CalculationWizard(ProductSystem productSystem) {
		this.productSystem = productSystem;
		setNeedsProgressMonitor(true);
		setWindowTitle(Messages.CalculationWizardTitle);
	}

	@Override
	public void addPages() {
		calculationPage = new CalculationWizardPage(productSystem);
		addPage(calculationPage);
	}

	@Override
	public boolean performFinish() {
		CalculationSetup settings = calculationPage.getSetup();
		try {
			getContainer().run(true, true, new Calculation(settings));
			return true;
		} catch (Exception e) {
			log.error("Calculation failed", e);
			return false;
		}
	}

	private class Calculation implements IRunnableWithProgress {

		private CalculationSetup setup;

		public Calculation(CalculationSetup settings) {
			this.setup = settings;
		}

		@Override
		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			monitor.beginTask("Run calculation", IProgressMonitor.UNKNOWN);
			if (setup.hasType(CalculationSetup.QUICK_RESULT))
				solve();
			else if (setup.hasType(CalculationSetup.ANALYSIS))
				analyse();
			else if (setup.hasType(CalculationSetup.MONTE_CARLO_SIMULATION))
				simulate();
			monitor.done();
		}

		private void analyse() {
			log.trace("run analysis");
			SystemCalculator calculator = new SystemCalculator(Database.get());
			AnalysisResult analysisResult = calculator.analyse(setup);
			log.trace("calculation done, open editor");
			String resultKey = App.getCache().put(analysisResult);
			String setupKey = App.getCache().put(setup);
			AnalyzeEditorInput input = new AnalyzeEditorInput(setupKey,
					resultKey);
			Editors.open(input, AnalyzeEditor.ID);
		}

		private void solve() {
			log.trace("run quick calculation");
			SystemCalculator calculator = new SystemCalculator(Database.get());
			InventoryResult inventoryResult = calculator.solve(setup);
			log.trace("calculation done, open editor");
			String resultKey = App.getCache().put(inventoryResult);
			String setupKey = App.getCache().put(setup);
			InventoryResultInput input = new InventoryResultInput(setup
					.getProductSystem().getId(), resultKey, setupKey);
			Editors.open(input, InventoryResultEditor.ID);
		}

		private void simulate() {
			log.trace("init Monte Carlo Simulation");
			SimulationInit init = new SimulationInit(setup, Database.get());
			init.run();
		}
	}
}
