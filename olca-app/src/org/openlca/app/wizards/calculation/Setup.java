package org.openlca.app.wizards.calculation;

import org.openlca.app.db.Database;
import org.openlca.app.preferences.FeatureFlag;
import org.openlca.app.preferences.Preferences;
import org.openlca.core.database.IDatabase;
import org.openlca.core.math.data_quality.AggregationType;
import org.openlca.core.math.data_quality.DQCalculationSetup;
import org.openlca.core.math.data_quality.NAHandling;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.CalculationSetup;
import org.openlca.core.model.CalculationType;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.NwSet;
import org.openlca.core.model.ParameterRedefSet;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.model.descriptors.NwSetDescriptor;
import org.openlca.util.ProductSystems;
import org.openlca.util.Strings;
import org.slf4j.LoggerFactory;

class Setup {

	final boolean hasLibraries;
	final CalculationSetup calcSetup;
	final DQCalculationSetup dqSetup;
	private final IDatabase db = Database.get();

	boolean storeInventory;
	boolean withDataQuality;

	private Setup(ProductSystem system) {
		hasLibraries = FeatureFlag.LIBRARIES.isEnabled()
			&& ProductSystems.hasLibraryLinks(system, Database.get());
		calcSetup = CalculationSetup.contributions(system);
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
		calcSetup.parameterRedefs.addAll(params.parameters);
	}

	boolean hasType(CalculationType type) {
		return calcSetup.calculationType == type;
	}

	void setType(CalculationType type) {
		calcSetup.calculationType = type;
	}

	void setMethod(ImpactMethodDescriptor method) {
		calcSetup.nwSet = null;
		if (method == null) {
			calcSetup.impactMethod = null;
			return;
		}
		calcSetup.impactMethod = db.get(ImpactMethod.class, method.id);
	}

	void setNwSet(NwSetDescriptor nwSet) {
		calcSetup.nwSet = null;
		if (nwSet == null)
			return;
		var method = calcSetup.impactMethod;
		if (method == null)
			return;
		calcSetup.nwSet = method.nwSets.stream()
			.filter(n -> n.id == nwSet.id)
			.findAny()
			.orElse(null);
	}

	/**
	 * Initializes a calculation setup with the stored preferences of the last
	 * calculation.
	 */
	static Setup init(ProductSystem system) {
		Setup s = new Setup(system);
		s.calcSetup.calculationType = loadEnumPref(CalculationType.class,
			CalculationType.CONTRIBUTION_ANALYSIS);
		s.calcSetup.allocationMethod = loadAllocationPref();
		s.calcSetup.impactMethod = loadImpactMethodPref();
		s.calcSetup.nwSet = loadNwSetPref(s.calcSetup.impactMethod);
		s.calcSetup.numberOfRuns = Preferences.getInt("calc.numberOfRuns");
		s.calcSetup.withRegionalization = Preferences.getBool("calc.regionalized");
		s.calcSetup.withCosts = Preferences.getBool("calc.costCalculation");

		// data quality settings
		s.withDataQuality = false; // Preferences.getBool("calc.dqAssessment");
		s.dqSetup.aggregationType = loadEnumPref(
			AggregationType.class, AggregationType.WEIGHTED_AVERAGE);
		s.dqSetup.naHandling = loadEnumPref(
			NAHandling.class, NAHandling.EXCLUDE);
		s.dqSetup.ceiling = Preferences.getBool("calc.dqCeiling");
		// init the DQ systems from the ref. process
		if (system.referenceProcess != null) {
			var p = system.referenceProcess;
			s.dqSetup.exchangeSystem = p.exchangeDqSystem;
			s.dqSetup.processSystem = p.dqSystem;
		}

		// reset features that are currently not supported with libraries
		if (s.hasLibraries) {
			s.calcSetup.nwSet = null;
			s.calcSetup.withCosts = false;
			s.withDataQuality = false;
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

	private static ImpactMethod loadImpactMethodPref() {
		var refId = Preferences.get("calc.impact.method");
		if (Strings.nullOrEmpty(refId))
			return null;
		try {
			return Database.get().get(ImpactMethod.class, refId);
		} catch (Exception e) {
			var log = LoggerFactory.getLogger(Setup.class);
			log.error("failed to load LCIA method", e);
			return null;
		}
	}

	private static NwSet loadNwSetPref(ImpactMethod method) {
		if (method == null || method.nwSets.isEmpty())
			return null;
		var nwSetRefId = Preferences.get("calc.nwset");
		if (nwSetRefId == null || nwSetRefId.isEmpty())
			return null;
		try {
			return method.nwSets.stream()
				.filter(nwSet -> nwSetRefId.equals(nwSet.refId))
				.findAny()
				.orElse(null);
		} catch (Exception e) {
			var log = LoggerFactory.getLogger(Setup.class);
			log.error("failed to load NW sets", e);
		}
		return null;
	}

	private static <T extends Enum<T>> T loadEnumPref(
		Class<T> type, T defaultVal) {
		String name = Preferences.get(
			"calc." + type.getSimpleName());
		if (Strings.nullOrEmpty(name))
			return defaultVal;
		try {
			return Enum.valueOf(type, name);
		} catch (Exception ignored) {
		}
		return defaultVal;
	}

	void savePreferences() {
		if (calcSetup == null)
			return;
		savePreference(CalculationType.class, calcSetup.calculationType);

		// allocation method
		AllocationMethod am = calcSetup.allocationMethod;
		Preferences.set("calc.allocation.method",
			am == null ? "NONE" : am.name());

		// LCIA method
		var m = calcSetup.impactMethod;
		Preferences.set("calc.impact.method", m == null ? "" : m.refId);

		// NW set
		var nws = calcSetup.nwSet;
		Preferences.set("calc.nwset", nws == null ? "" : nws.refId);

		// calculation options
		Preferences.set("calc.numberOfRuns", calcSetup.numberOfRuns);
		Preferences.set("calc.costCalculation", calcSetup.withCosts);
		Preferences.set("calc.regionalized", calcSetup.withRegionalization);

		// data quality settings
		if (!withDataQuality) {
			Preferences.set("calc.dqAssessment", false);
			return;
		}
		Preferences.set("calc.dqAssessment", true);
		Preferences.set("calc.dqCeiling", dqSetup.ceiling);
		savePreference(AggregationType.class, dqSetup.aggregationType);
		savePreference(NAHandling.class, dqSetup.naHandling);
	}

	private <T extends Enum<T>> void savePreference(Class<T> clazz, T value) {
		Preferences.set("calc." + clazz.getSimpleName(),
			value == null ? null : value.name());
	}

}
