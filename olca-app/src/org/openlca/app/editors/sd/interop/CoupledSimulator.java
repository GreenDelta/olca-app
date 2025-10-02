package org.openlca.app.editors.sd.interop;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.openlca.app.App;
import org.openlca.app.db.Database;
import org.openlca.app.db.Libraries;
import org.openlca.core.math.SystemCalculator;
import org.openlca.core.model.CalculationSetup;
import org.openlca.core.model.ParameterRedef;
import org.openlca.sd.eqn.SimulationState;
import org.openlca.sd.eqn.Simulator;
import org.openlca.sd.eqn.cells.NumCell;
import org.openlca.sd.xmile.Xmile;
import org.openlca.util.Res;

public class CoupledSimulator {

	private final Simulator simulator;
	private final SimulationSetup setup;
	private final SystemCalculator calculator;
	private final CoupledResult result;

	private CoupledSimulator(
			Simulator simulator, SimulationSetup setup, SystemCalculator calculator
	) {
		this.simulator = simulator;
		this.setup = setup;
		this.calculator = calculator;
		this.result = new CoupledResult();
	}

	public static Res<CoupledSimulator> of(
			Xmile xmile, SimulationSetup setup
	) {
		var simulator = Simulator.of(xmile);
		if (simulator.hasError())
			return simulator.wrapError("failed to create simulator");

		var calculator = new SystemCalculator(Database.get())
				.withSolver(App.getSolver());
		Libraries.readersForCalculation()
				.ifPresent(calculator::withLibraries);
		return Res.of(new CoupledSimulator(simulator.value(), setup, calculator));
	}

	public CoupledResult getResult() {
		return result;
	}

	public void forEach(Consumer<Res<CoupledResult>> fn) {
		simulator.forEach(res -> {
			if (res.hasError()) {
				fn.accept(res.wrapError("Simulation error"));
				return;
			}
			var simState = res.value();
			var lcaResults = new ArrayList<org.openlca.core.results.LcaResult>();

			for (var b : setup.systemBindings()) {
				var params = paramsOf(simState, b);
				if (params.hasError()) {
					fn.accept(params.wrapError("Variable binding error"));
					return;
				}

				var calcSetup = CalculationSetup.of(b.system())
						.withParameters(params.value())
						.withAllocation(b.allocation())
						.withImpactMethod(setup.method())
						.withAmount(b.amount()); // TODO: the amount can be bound to a var

				try {
					var lcaResult = calculator.calculate(calcSetup);
					lcaResults.add(lcaResult);
				} catch (Exception e) {
					fn.accept(Res.error(
							"Calculation of system failed: " + b.system().name, e));
					return;
				}
			}

			// Update the coupled result with simulation state and LCA results
			result.append(simState, lcaResults);
			fn.accept(Res.of(result));
		});
	}

	private Res<List<ParameterRedef>> paramsOf(
			SimulationState simState, SystemBinding binding
	) {
		var params = new ArrayList<ParameterRedef>();
		for (var vb : binding.varBindings()) {
			if (vb.varId() == null || vb.parameter() == null)
				continue;
			var cell = simState.valueOf(vb.varId()).orElse(null);
			if (cell == null)
				return Res.error("Variable not found: " + vb.varId());
			if (!(cell instanceof NumCell(double num))) {
				return Res.error("Variable does not evaluate to a number: "
						+ vb.varId());
			}

			var param = vb.parameter().copy();
			param.value = num;
			params.add(param);
		}
		return Res.of(params);
	}
}
