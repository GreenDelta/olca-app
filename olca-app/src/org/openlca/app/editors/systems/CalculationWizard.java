package org.openlca.app.editors.systems;

import java.lang.reflect.InvocationTargetException;
import java.math.RoundingMode;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.Preferences;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.results.ResultEditorInput;
import org.openlca.app.results.analysis.AnalyzeEditor;
import org.openlca.app.results.quick.QuickResultEditor;
import org.openlca.app.results.regionalized.RegionalizedResultEditor;
import org.openlca.app.results.simulation.SimulationInit;
import org.openlca.app.util.Editors;
import org.openlca.app.util.Info;
import org.openlca.app.util.UI;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.SystemCalculator;
import org.openlca.core.math.data_quality.AggregationType;
import org.openlca.core.math.data_quality.DQCalculationSetup;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.math.data_quality.ProcessingType;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.results.ContributionResult;
import org.openlca.core.results.ContributionResultProvider;
import org.openlca.core.results.FullResult;
import org.openlca.core.results.FullResultProvider;
import org.openlca.geo.RegionalizedCalculator;
import org.openlca.geo.RegionalizedResult;
import org.openlca.geo.RegionalizedResultProvider;
import org.openlca.geo.parameter.ParameterSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wizard for setting calculation properties and run the calculation of a
 * product system
 */
public class CalculationWizard extends Wizard {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	CalculationWizardPage calculationPage;
	DQSettingsPage dqSettingsPage;
	ProductSystem productSystem;

	public CalculationWizard(ProductSystem productSystem) {
		this.productSystem = productSystem;
		setNeedsProgressMonitor(true);
		setWindowTitle(M.CalculationProperties);
	}

	public static void open(ProductSystem productSystem) {
		if (productSystem == null)
			return;
		CalculationWizard wizard = new CalculationWizard(productSystem);
		WizardDialog dialog = new WizardDialog(UI.shell(), wizard);
		dialog.open();
	}

	@Override
	public void addPages() {
		calculationPage = new CalculationWizardPage();
		addPage(calculationPage);
		dqSettingsPage = new DQSettingsPage();
		addPage(dqSettingsPage);
	}

	@Override
	public boolean performFinish() {
		CalculationSetup setup = calculationPage.getSetup(productSystem);
		CalculationType type = calculationPage.getCalculationType();
		DQCalculationSetup dqSetup = null;
		if (calculationPage.doDqAssessment())
			dqSetup = dqSettingsPage.getSetup(productSystem);
		saveDefaults(setup, dqSetup, type);
		try {
			Calculation calculation = new Calculation(setup, type, dqSetup);
			getContainer().run(true, true, calculation);
			if (calculation.outOfMemory)
				MemoryError.show();
			return !calculation.outOfMemory;
		} catch (Exception e) {
			log.error("Calculation failed", e);
			return false;
		}
	}

	private ResultEditorInput getEditorInput(Object result, CalculationSetup setup,
			ParameterSet parameterSet, DQResult dqResult) {
		ResultEditorInput input = ResultEditorInput.create(setup, result)
				.with(dqResult)
				.with(parameterSet);
		return input;
	}

	private void saveDefaults(CalculationSetup setup, DQCalculationSetup dqSetup, CalculationType type) {
		if (setup == null)
			return;
		AllocationMethod am = setup.allocationMethod;
		String amVal = am == null ? "NONE" : am.name();
		Preferences.set("calc.allocation.method", amVal);
		BaseDescriptor m = setup.impactMethod;
		String mVal = m == null ? "" : m.getRefId();
		Preferences.set("calc.impact.method", mVal);
		BaseDescriptor nws = setup.nwSet;
		String nwsVal = nws == null ? "" : nws.getRefId();
		Preferences.set("calc.nwset", nwsVal);
		saveDefault(CalculationType.class, type);
		Preferences.set("calc.numberOfRuns", Integer.toString(setup.numberOfRuns));
		Preferences.set("calc.costCalculation", Boolean.toString(setup.withCosts));
		if (dqSetup == null) {
			Preferences.set("calc.dqAssessment", "false");
			return;
		}
		Preferences.set("calc.dqAssessment", "true");
		saveDefault(AggregationType.class, dqSetup.aggregationType);
		saveDefault(ProcessingType.class, dqSetup.processingType);
		saveDefault(RoundingMode.class, dqSetup.roundingMode);
	}

	private <T extends Enum<T>> void saveDefault(Class<T> clazz, T value) {
		Preferences.set("calc." + clazz.getSimpleName(), value == null ? null : value.name());
	}

	private class Calculation implements IRunnableWithProgress {

		private CalculationSetup setup;
		private CalculationType type;
		private DQCalculationSetup dqSetup;
		private boolean outOfMemory;

		public Calculation(CalculationSetup setup, CalculationType type, DQCalculationSetup dqSetup) {
			this.setup = setup;
			this.type = type;
			this.dqSetup = dqSetup;
		}

		@Override
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			outOfMemory = false;
			monitor.beginTask(M.RunCalculation, IProgressMonitor.UNKNOWN);
			int size = productSystem.getProcesses().size();
			log.trace("calculate a {} x {} system", size, size);
			try {
				switch (type) {
				case ANALYSIS:
					analyse();
					break;
				case MONTE_CARLO:
					simulate();
					break;
				case QUICK:
					solve();
					break;
				case REGIONALIZED:
					calcRegionalized();
					break;
				default:
					break;
				}
			} catch (OutOfMemoryError e) {
				outOfMemory = true;
			}
			monitor.done();
		}

		private void analyse() {
			log.trace("run analysis");
			SystemCalculator calculator = new SystemCalculator(Cache.getMatrixCache(), App.getSolver());
			FullResult result = calculator.calculateFull(setup);
			log.trace("calculation done, open editor");
			FullResultProvider resultProvider = new FullResultProvider(result, Cache.getEntityCache());
			DQResult dqResult = DQResult.calculate(Database.get(), result, dqSetup);
			ResultEditorInput input = getEditorInput(resultProvider, setup, null, dqResult);
			Editors.open(input, AnalyzeEditor.ID);
		}

		private void solve() {
			log.trace("run quick calculation");
			SystemCalculator calculator = new SystemCalculator(Cache.getMatrixCache(), App.getSolver());
			ContributionResult result = calculator.calculateContributions(setup);
			log.trace("calculation done, open editor");
			ContributionResultProvider<ContributionResult> resultProvider = new ContributionResultProvider<>(result,
					Cache.getEntityCache());
			DQResult dqResult = DQResult.calculate(Database.get(), result, dqSetup);
			ResultEditorInput input = getEditorInput(resultProvider, setup, null, dqResult);
			Editors.open(input, QuickResultEditor.ID);
		}

		private void simulate() {
			log.trace("init Monte Carlo Simulation");
			SimulationInit init = new SimulationInit(setup, Cache.getMatrixCache());
			init.run();
		}

		private void calcRegionalized() {
			log.trace("calculate regionalized result");
			RegionalizedCalculator calculator = new RegionalizedCalculator(setup, App.getSolver());
			RegionalizedResult regioResult = calculator.calculate(Database.get(), Cache.getMatrixCache());
			if (regioResult == null) {
				Info.showBox("No regionalized information available for this system");
				return;
			}
			RegionalizedResultProvider provider = new RegionalizedResultProvider();
			provider.result = new FullResultProvider(regioResult.result, Cache.getEntityCache());
			provider.kmlData = regioResult.kmlData;
			DQResult dqResult = DQResult.calculate(Database.get(), regioResult.result, dqSetup);
			ResultEditorInput input = getEditorInput(provider, setup, regioResult.parameterSet, dqResult);
			Editors.open(input, RegionalizedResultEditor.ID);
		}

	}
}
