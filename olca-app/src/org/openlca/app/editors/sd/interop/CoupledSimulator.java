package org.openlca.app.editors.sd.interop;

import java.util.function.Consumer;

import org.openlca.core.model.CalculationSetup;
import org.openlca.sd.eqn.Simulator;
import org.openlca.sd.xmile.Xmile;
import org.openlca.util.Res;

public class CoupledSimulator {

	private final Simulator simulator;
	private final SimulationSetup setup;

	private CoupledSimulator(Simulator simulator, SimulationSetup setup) {
		this.simulator = simulator;
		this.setup = setup;
	}

	public static Res<CoupledSimulator> of(
			Xmile xmile, SimulationSetup setup
	) {
		var simulator = Simulator.of(xmile);
		if (simulator.hasError())
			return simulator.wrapError("failed to create simulator");
		return Res.of(new CoupledSimulator(simulator.value(), setup));
	}

	public void forEach(Consumer<Res<CoupledResult>> fn) {
		simulator.forEach(res -> {
			if (res.hasError()) {
				fn.accept(res.wrapError("simulation error"));
				return;
			}

			for (var b : setup.systemBindings()) {
				var calcSetup = CalculationSetup.of(b.system())
						.withAllocation(b.allocation())
						.withImpactMethod(setup.method())
						.withAmount(b.amount()); // TODO: the amount can be bound to a var

			}

		});
	}

}
