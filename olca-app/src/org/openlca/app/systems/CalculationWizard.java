package org.openlca.app.systems;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.db.Cache;
import org.openlca.app.results.analysis.AnalyzeEditor;
import org.openlca.app.results.analysis.AnalyzeEditorInput;
import org.openlca.app.results.quick.QuickResultEditor;
import org.openlca.app.results.quick.QuickResultInput;
import org.openlca.app.simulation.SimulationInit;
import org.openlca.app.util.Editors;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.SystemCalculator;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.results.ContributionResult;
import org.openlca.core.results.FullResult;
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
			int size = productSystem.getProcesses().size();
			log.trace("calculate a {} x {} system", size, size);
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
			SystemCalculator calculator = new SystemCalculator(
					Cache.getMatrixCache(), App.getSolver());
			FullResult analysisResult = calculator.calculateFull(setup);
			log.trace("calculation done, open editor");
			String resultKey = Cache.getAppCache().put(analysisResult);
			String setupKey = Cache.getAppCache().put(setup);
			AnalyzeEditorInput input = new AnalyzeEditorInput(setupKey,
					resultKey);
			Editors.open(input, AnalyzeEditor.ID);
		}

		private void solve() {
			log.trace("run quick calculation");
			SystemCalculator calculator = new SystemCalculator(
					Cache.getMatrixCache(), App.getSolver());
			ContributionResult inventoryResult = calculator
					.calculateContributions(setup);
			log.trace("calculation done, open editor");
			String resultKey = Cache.getAppCache().put(inventoryResult);
			String setupKey = Cache.getAppCache().put(setup);
			QuickResultInput input = new QuickResultInput(setup
					.getProductSystem().getId(), resultKey, setupKey);
			Editors.open(input, QuickResultEditor.ID);
		}

		private void simulate() {
			log.trace("init Monte Carlo Simulation");
			SimulationInit init = new SimulationInit(setup,
					Cache.getMatrixCache());
			init.run();
		}
	}
}
