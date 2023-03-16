package org.openlca.app.wizards.calculation;

import org.openlca.app.db.Database;
import org.openlca.core.database.IDatabase;
import org.openlca.core.math.data_quality.DQSetup;
import org.openlca.core.model.CalculationSetup;
import org.openlca.core.model.CalculationTarget;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.NwSet;
import org.openlca.core.model.ParameterRedefSet;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.util.ProductSystems;

class Setup {

	private final IDatabase db = Database.get();
	final boolean hasLibraries;
	final CalculationSetup calcSetup;
	final DQSetup dqSetup;

	boolean withDataQuality;
	CalculationType type;
	int simulationRuns = 100;

	private Setup(CalculationTarget target) {
		var system = target.isProductSystem()
				? target.asProductSystem()
				: null;
		hasLibraries = system != null
				&& ProductSystems.hasLibraryLinks(system, db);

		calcSetup = CalculationSetup.of(target);
		dqSetup = new DQSetup();

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
		var setup = new Setup(target);
		CalculationPreferences.apply(setup, Database.get());
		// disable features that are currently not supported with libraries
		if (setup.hasLibraries) {
			setup.calcSetup.withCosts(false);
			setup.withDataQuality = false;
		}
		return setup;
	}

	void savePreferences() {
		CalculationPreferences.save(this, db);
	}
}
