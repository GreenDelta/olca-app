package org.openlca.app.wizards.calculation;

import java.math.RoundingMode;

import org.openlca.app.Preferences;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.CalculationType;
import org.openlca.core.math.data_quality.AggregationType;
import org.openlca.core.math.data_quality.DQCalculationSetup;
import org.openlca.core.math.data_quality.ProcessingType;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.BaseDescriptor;

class Setup {

	CalculationSetup calcSetup;
	CalculationType calcType;
	DQCalculationSetup dqSetup;
	boolean storeInventory;

	/**
	 * Initializes a calculation setup with the stored preferences of the last
	 * calculation.
	 */
	static Setup init(ProductSystem system) {
		Setup setup = new Setup();

		return setup;
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
		if (dqSetup == null) {
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
