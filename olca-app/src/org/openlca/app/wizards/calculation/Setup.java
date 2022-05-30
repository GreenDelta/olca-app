package org.openlca.app.wizards.calculation;

import org.openlca.app.db.Database;
import org.openlca.app.preferences.Preferences;
import org.openlca.core.database.IDatabase;
import org.openlca.core.math.data_quality.AggregationType;
import org.openlca.core.math.data_quality.DQCalculationSetup;
import org.openlca.core.math.data_quality.NAHandling;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.CalculationSetup;
import org.openlca.core.model.CalculationTarget;
import org.openlca.core.model.CalculationType;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.NwSet;
import org.openlca.core.model.ParameterRedefSet;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.util.ProductSystems;
import org.openlca.util.Strings;
import org.slf4j.LoggerFactory;

class Setup {

	final boolean hasLibraries;
	final CalculationSetup calcSetup;
	final DQCalculationSetup dqSetup;
	private final IDatabase db = Database.get();
	boolean withDataQuality;

	private Setup(CalculationTarget target) {
		var system = target.isProductSystem()
			? target.asProductSystem()
			: null;
		hasLibraries = system != null
			&& ProductSystems.hasLibraryLinks(system, db);

		calcSetup = CalculationSetup.contributions(target);
		dqSetup = new DQCalculationSetup();
		// dqSetup.productSystemId = target.id;

		// add parameter redefinitions
		if (system != null && system.parameterSets.size() > 0) {
			var baseline = system.parameterSets.stream()
				.filter(ps -> ps.isBaseline)
				.findFirst()
				.orElse(system.parameterSets.get(0));
			if (baseline != null) {
				calcSetup.withParameters(baseline.parameters);
			}
		}
	}

	void setParameters(ParameterRedefSet params) {
		calcSetup.withParameters(params.parameters);
	}

	boolean hasType(CalculationType type) {
		return calcSetup.type() == type;
	}

	void setType(CalculationType type) {
		calcSetup.withType(type);
	}

	void setMethod(ImpactMethodDescriptor method) {
		calcSetup.withNwSet(null);
		if (method == null) {
			calcSetup.withImpactMethod(null);
			return;
		}
		calcSetup.withImpactMethod(db.get(ImpactMethod.class, method.id));
	}

	void setNwSet(NwSet nwSet) {
		calcSetup.withNwSet(null);
		if (nwSet == null)
			return;
		var method = calcSetup.impactMethod();
		if (method == null)
			return;
		calcSetup.withNwSet(nwSet);
	}

	/**
	 * Initializes a calculation setup with the stored preferences of the last
	 * calculation.
	 */
	static Setup init(CalculationTarget target) {
		Setup s = new Setup(target);
		s.calcSetup.withType(
				loadEnumPref(CalculationType.class, CalculationType.CONTRIBUTION_ANALYSIS))
			.withAllocation(loadAllocationPref())
			.withImpactMethod(loadImpactMethodPref())
			.withNwSet(loadNwSetPref(s.calcSetup.impactMethod()))
			.withNumberOfRuns(Preferences.getInt("calc.numberOfRuns"))
			.withRegionalization(Preferences.getBool("calc.regionalized"))
			.withCosts(Preferences.getBool("calc.costCalculation"));

		// data quality settings
		s.withDataQuality = false; // Preferences.getBool("calc.dqAssessment");
		s.dqSetup.aggregationType = loadEnumPref(
			AggregationType.class, AggregationType.WEIGHTED_AVERAGE);
		s.dqSetup.naHandling = loadEnumPref(
			NAHandling.class, NAHandling.EXCLUDE);
		s.dqSetup.ceiling = Preferences.getBool("calc.dqCeiling");
		// init the DQ systems from the ref. process
		var process = s.calcSetup.process();
		if (process != null) {
			s.dqSetup.exchangeSystem = process.exchangeDqSystem;
			s.dqSetup.processSystem = process.dqSystem;
		}

		// reset features that are currently not supported with libraries
		if (s.hasLibraries) {
			s.calcSetup.withCosts(false);
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
		savePreference(CalculationType.class, calcSetup.type());

		// allocation method
		AllocationMethod am = calcSetup.allocation();
		Preferences.set("calc.allocation.method",
			am == null ? "NONE" : am.name());

		// LCIA method
		var m = calcSetup.impactMethod();
		Preferences.set("calc.impact.method", m == null ? "" : m.refId);

		// NW set
		var nws = calcSetup.nwSet();
		Preferences.set("calc.nwset", nws == null ? "" : nws.refId);

		// calculation options
		Preferences.set("calc.numberOfRuns", calcSetup.numberOfRuns());
		Preferences.set("calc.costCalculation", calcSetup.hasCosts());
		Preferences.set("calc.regionalized", calcSetup.hasRegionalization());

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
