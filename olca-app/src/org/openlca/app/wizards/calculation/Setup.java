package org.openlca.app.wizards.calculation;

import java.math.RoundingMode;

import org.apache.poi.ss.formula.functions.T;
import org.openlca.app.Preferences;
import org.openlca.app.db.Database;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.database.NwSetDao;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.CalculationType;
import org.openlca.core.math.data_quality.AggregationType;
import org.openlca.core.math.data_quality.DQCalculationSetup;
import org.openlca.core.math.data_quality.ProcessingType;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.ParameterRedefSet;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.model.descriptors.NwSetDescriptor;
import org.python.google.common.base.Strings;
import org.slf4j.LoggerFactory;

class Setup {

	final CalculationSetup calcSetup;
	final DQCalculationSetup dqSetup;
	CalculationType calcType;
	boolean storeInventory;
	boolean withDataQuality;

	private Setup(ProductSystem system) {
		calcSetup = new CalculationSetup(system);
		dqSetup = new DQCalculationSetup();
		dqSetup.productSystemId = system.id;

		// add parameter redefinitions
		if (system.parameterSets.size() > 0) {
			ParameterRedefSet baseline = system.parameterSets
					.stream()
					.filter(ps -> ps.isBaseline)
					.findFirst()
					.orElse(system.parameterSets.get(0));
			if (baseline != null) {
				calcSetup.parameterRedefs.addAll(
						baseline.parameters);
			}
		}
	}

	void setParameters(ParameterRedefSet params) {
		calcSetup.parameterRedefs.clear();
		if (params == null)
			return;
		calcSetup.parameterRedefs.addAll(
				params.parameters);
	}

	/**
	 * Initializes a calculation setup with the stored preferences of the last
	 * calculation.
	 */
	static Setup init(ProductSystem system) {
		Setup s = new Setup(system);
		s.calcType = loadEnumPref(CalculationType.class,
				CalculationType.CONTRIBUTION_ANALYSIS);
		s.calcSetup.allocationMethod = loadAllocationPref();
		s.calcSetup.impactMethod = loadImpactMethodPref();
		s.calcSetup.nwSet = loadNwSetPref(s.calcSetup.impactMethod);
		s.calcSetup.numberOfRuns = Preferences.getInt("calc.numberOfRuns");
		s.calcSetup.withRegionalization = loadBooleanPref("calc.regionalized");
		s.calcSetup.withCosts = loadBooleanPref("calc.costCalculation");

		// data quality settings
		s.withDataQuality = loadBooleanPref("calc.dqAssessment");
		s.dqSetup.aggregationType = loadEnumPref(
				AggregationType.class, AggregationType.WEIGHTED_AVERAGE);
		s.dqSetup.processingType = loadEnumPref(
				ProcessingType.class, ProcessingType.EXCLUDE);
		s.dqSetup.roundingMode = loadEnumPref(
				RoundingMode.class, RoundingMode.HALF_UP);
		// init the DQ systems from the ref. process
		if (system.referenceProcess != null) {
			var p = system.referenceProcess;
			s.dqSetup.exchangeDqSystem = p.exchangeDqSystem;
			s.dqSetup.processDqSystem = p.dqSystem;
		}
		
		return s;
	}

	private static AllocationMethod loadAllocationPref() {
		String val = Preferences.get("calc.allocation.method");
		if (val == null)
			return AllocationMethod.NONE;
		for (AllocationMethod method : AllocationMethod.values()) {
			if (method.name().equals(val))
				return method;
		}
		return AllocationMethod.NONE;
	}

	private static ImpactMethodDescriptor loadImpactMethodPref() {
		String val = Preferences.get("calc.impact.method");
		if (val == null || val.isEmpty())
			return null;
		try {
			ImpactMethodDao dao = new ImpactMethodDao(Database.get());
			for (ImpactMethodDescriptor d : dao.getDescriptors()) {
				if (val.equals(d.refId))
					return d;
			}
		} catch (Exception e) {
			LoggerFactory.getLogger(Setup.class).error(
					"failed to load LCIA methods", e);
		}
		return null;
	}

	private static NwSetDescriptor loadNwSetPref(ImpactMethodDescriptor method) {
		if (method == null)
			return null;
		String val = Preferences.get("calc.nwset");
		if (val == null || val.isEmpty())
			return null;
		try {
			NwSetDao dao = new NwSetDao(Database.get());
			for (NwSetDescriptor d : dao.getDescriptorsForMethod(method.id)) {
				if (val.equals(d.refId))
					return d;
			}
		} catch (Exception e) {
			LoggerFactory.getLogger(Setup.class).error(
					"failed to load NW sets", e);
		}
		return null;
	}

	private static boolean loadBooleanPref(String option) {
		String value = Preferences.get(option);
		if (value == null)
			return false;
		return "true".equals(value.toLowerCase());
	}

	private static <T extends Enum<T>> T loadEnumPref(
			Class<T> type, T defaultVal) {
		String name = Preferences.get(
				"calc." + type.getSimpleName());
		if (Strings.isNullOrEmpty(name))
			return defaultVal;
		try {
			T value = Enum.valueOf(type, name);
			if (value != null)
				return value;
		} catch (Exception e) {
		}
		return defaultVal;
	}

	void savePreferences() {

		savePreference(CalculationType.class, calcType);

		if (calcSetup == null)
			return;

		// allocation method
		AllocationMethod am = calcSetup.allocationMethod;
		Preferences.set("calc.allocation.method",
				am == null ? "NONE" : am.name());

		// LCIA method
		BaseDescriptor m = calcSetup.impactMethod;
		Preferences.set("calc.impact.method",
				m == null ? "" : m.refId);

		// NW set
		BaseDescriptor nws = calcSetup.nwSet;
		Preferences.set("calc.nwset",
				nws == null ? "" : nws.refId);

		// calculation options
		Preferences.set("calc.numberOfRuns",
				Integer.toString(calcSetup.numberOfRuns));
		Preferences.set("calc.costCalculation",
				Boolean.toString(calcSetup.withCosts));
		Preferences.set("calc.regionalized",
				Boolean.toString(calcSetup.withRegionalization));

		// data quality settings
		if (!withDataQuality) {
			Preferences.set("calc.dqAssessment", "false");
			return;
		}
		Preferences.set("calc.dqAssessment", "true");
		savePreference(AggregationType.class, dqSetup.aggregationType);
		savePreference(ProcessingType.class, dqSetup.processingType);
		savePreference(RoundingMode.class, dqSetup.roundingMode);
	}

	private <T extends Enum<T>> void savePreference(Class<T> clazz, T value) {
		Preferences.set(
				"calc." + clazz.getSimpleName(),
				value == null ? null : value.name());
	}

}
