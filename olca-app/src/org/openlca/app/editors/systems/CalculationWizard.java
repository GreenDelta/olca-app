package org.openlca.app.editors.systems;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.Preferences;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.editors.lcia_methods.ShapeFileUtils;
import org.openlca.app.results.ResultEditorInput;
import org.openlca.app.results.analysis.AnalyzeEditor;
import org.openlca.app.results.quick.QuickResultEditor;
import org.openlca.app.results.regionalized.RegionalizedResultEditor;
import org.openlca.app.results.simulation.SimulationInit;
import org.openlca.app.util.Editors;
import org.openlca.app.util.Info;
import org.openlca.app.util.UI;
import org.openlca.core.database.IDatabase;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.DataStructures;
import org.openlca.core.math.IMatrixSolver;
import org.openlca.core.math.LcaCalculator;
import org.openlca.core.math.SystemCalculator;
import org.openlca.core.matrix.ImpactMatrix;
import org.openlca.core.matrix.ImpactTable;
import org.openlca.core.matrix.Inventory;
import org.openlca.core.matrix.InventoryMatrix;
import org.openlca.core.matrix.ParameterTable;
import org.openlca.core.matrix.ProductIndex;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.results.ContributionResult;
import org.openlca.core.results.ContributionResultProvider;
import org.openlca.core.results.FullResult;
import org.openlca.core.results.FullResultProvider;
import org.openlca.expressions.FormulaInterpreter;
import org.openlca.geo.RegionalizationSetup;
import org.openlca.geo.RegionalizedCalculator;
import org.openlca.geo.RegionalizedResult;
import org.openlca.geo.RegionalizedResultProvider;
import org.openlca.geo.kml.IKmlLoader;
import org.openlca.geo.kml.KmlLoader;
import org.openlca.geo.parameter.ParameterSet;
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
		setWindowTitle(Messages.CalculationProperties);
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
		calculationPage = new CalculationWizardPage(productSystem);
		addPage(calculationPage);
	}

	@Override
	public boolean performFinish() {
		CalculationSetup setup = calculationPage.getSetup();
		setup.withCosts = true;
		CalculationType type = calculationPage.getCalculationType();
		saveDefaults(setup, type);
		try {
			Calculation calculation = new Calculation(setup, type);
			getContainer().run(true, true, calculation);
			if (calculation.outOfMemory)
				OOMError.show();
			return calculation.done && !calculation.outOfMemory;
		} catch (Exception e) {
			log.error("Calculation failed", e);
			return false;
		}
	}

	private ResultEditorInput getEditorInput(Object result,
			CalculationSetup setup, ParameterSet parameterSet) {
		String resultKey = Cache.getAppCache().put(result);
		String setupKey = Cache.getAppCache().put(setup);
		String parameterSetKey = null;
		if (parameterSet != null)
			parameterSetKey = Cache.getAppCache().put(parameterSet);
		return new ResultEditorInput(setup.productSystem.getId(), resultKey,
				setupKey, parameterSetKey);
	}

	private void saveDefaults(CalculationSetup setup, CalculationType type) {
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
	}

	private class Calculation implements IRunnableWithProgress {

		private CalculationSetup setup;
		private CalculationType type;
		private boolean done;
		private boolean outOfMemory;

		public Calculation(CalculationSetup setup, CalculationType type) {
			this.setup = setup;
			this.type = type;
		}

		@Override
		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			outOfMemory = false;
			monitor.beginTask(Messages.RunCalculation, IProgressMonitor.UNKNOWN);
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
			SystemCalculator calculator = new SystemCalculator(
					Cache.getMatrixCache(), App.getSolver());
			FullResult result = calculator.calculateFull(setup);
			log.trace("calculation done, open editor");
			FullResultProvider resultProvider = new FullResultProvider(result,
					Cache.getEntityCache());
			ResultEditorInput input = getEditorInput(resultProvider, setup,
					null);
			Editors.open(input, AnalyzeEditor.ID);
			done = true;
		}

		private void solve() {
			log.trace("run quick calculation");
			SystemCalculator calculator = new SystemCalculator(
					Cache.getMatrixCache(), App.getSolver());
			ContributionResult result = calculator
					.calculateContributions(setup);
			log.trace("calculation done, open editor");
			ContributionResultProvider<ContributionResult> resultProvider = new ContributionResultProvider<>(
					result, Cache.getEntityCache());
			ResultEditorInput input = getEditorInput(resultProvider, setup,
					null);
			Editors.open(input, QuickResultEditor.ID);
			done = true;
		}

		private void simulate() {
			log.trace("init Monte Carlo Simulation");
			SimulationInit init = new SimulationInit(setup,
					Cache.getMatrixCache());
			init.run();
			done = true;
		}

		private void calcRegionalized() {
			log.trace("init regionalized calculation");
			RegionalizedCalculation calculation = new RegionalizedCalculation(
					setup);
			calculation.run();
			done = true;
		}

	}

	private class RegionalizedCalculation {

		private IMatrixSolver solver = App.getSolver();
		private IDatabase database = Database.get();
		private CalculationSetup setup;

		public RegionalizedCalculation(CalculationSetup setup) {
			this.setup = setup;
		}

		public void run() {
			log.trace("calculate regionalized result");
			Inventory inventory = DataStructures.createInventory(setup,
					Cache.getMatrixCache());
			ParameterTable parameterTable = DataStructures
					.createParameterTable(database, setup, inventory);
			FormulaInterpreter interpreter = parameterTable.createInterpreter();
			InventoryMatrix inventoryMatrix = inventory.createMatrix(
					solver.getMatrixFactory(), interpreter);
			ImpactMatrix impactMatrix = null;
			ImpactTable impactTable = null;
			if (setup.impactMethod != null) {
				impactTable = ImpactTable.build(Cache.getMatrixCache(),
						setup.impactMethod.getId(), inventory.getFlowIndex());
				impactMatrix = impactTable.createMatrix(
						solver.getMatrixFactory(), interpreter);
			}
			LcaCalculator calculator = new LcaCalculator(solver,
					inventoryMatrix);
			calculator.setImpactMatrix(impactMatrix);
			FullResult baseResult = calculator.calculateFull();
			RegionalizationSetup regioSetup = setupRegio(baseResult.productIndex);
			if (regioSetup == null)
				return;
			RegionalizedResult result = calculate(baseResult, regioSetup,
					interpreter, impactTable);
			RegionalizedResultProvider resultProvider = new RegionalizedResultProvider();
			resultProvider.result = new FullResultProvider(
					result.regionalizedResult, Cache.getEntityCache());
			resultProvider.kmlData = regioSetup.getKmlData();
			ResultEditorInput input = getEditorInput(resultProvider, setup,
					regioSetup.getParameterSet());
			Editors.open(input, RegionalizedResultEditor.ID);
		}

		private RegionalizationSetup setupRegio(ProductIndex productIndex) {
			RegionalizationSetup regioSetup = new RegionalizationSetup(
					database, setup.impactMethod,
					ShapeFileUtils.getFolder(setup.impactMethod));
			IKmlLoader kmlLoader = new KmlLoader(database);
			if (!regioSetup.init(kmlLoader, productIndex)) {
				Info.showBox("No regionalized information available for this system");
				return null;
			}
			return regioSetup;
		}

		private RegionalizedResult calculate(FullResult baseResult,
				RegionalizationSetup regioSetup,
				FormulaInterpreter interpreter, ImpactTable impactTable) {
			RegionalizedCalculator calculator = new RegionalizedCalculator(
					solver);
			return calculator.calculate(regioSetup, baseResult, interpreter,
					impactTable);
		}

	}

}
