package org.openlca.app.editors.sd.interop;

import java.util.ArrayList;
import java.util.List;

import org.openlca.commons.Res;
import org.openlca.core.math.SystemCalculator;
import org.openlca.core.model.CalculationSetup;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.results.LcaResult;
import org.openlca.sd.eqn.SimulationState;
import org.openlca.sd.eqn.Simulator;
import org.openlca.sd.eqn.cells.NumCell;
import org.openlca.sd.xmile.Xmile;

public class CoupledSimulator {

	private final Simulator simulator;
	private final SimulationSetup setup;
	private final SystemCalculator calculator;
	private final CoupledResult result;
	private Res<?> error;

	private CoupledSimulator(
		Simulator simulator,
		SimulationSetup setup,
		SystemCalculator calculator) {
		this.simulator = simulator;
		this.setup = setup;
		this.calculator = calculator;
		this.result = new CoupledResult();
	}

	public static Res<CoupledSimulator> of(
		Xmile xmile, SimulationSetup setup, SystemCalculator calculator) {
		var simulator = Simulator.of(xmile);
		if (simulator.isError())
			return simulator.wrapError("Failed to create simulator");

		return Res.ok(new CoupledSimulator(simulator.value(), setup, calculator));
	}

	public Res<CoupledResult> getResult() {
		return error != null
				? error.castError()
				: Res.ok(result);
	}

	public interface Progress {
		void worked(int work);
		boolean isCanceled();
	}

	public void run(Progress progress) {
		for (var res : simulator) {
			if (progress.isCanceled())
				break;

			if (res.isError()) {
				error = res.wrapError("Simulation error");
				break;
			}

			var simState = res.value();
			var rs = new ArrayList<LcaResult>();
			for (var b : setup.systemBindings()) {
				if (progress.isCanceled())
					break;

				var params = paramsOf(simState, b);
				if (params.isError()) {
					error = params.wrapError("Variable binding error");
					break;
				}

				var calcSetup = CalculationSetup.of(b.system())
						.withParameters(params.value())
						.withAllocation(b.allocation())
						.withImpactMethod(setup.method())
						.withAmount(b.amount()); // TODO: the amount can be bound to a var

				try {
					var lcaResult = calculator.calculate(calcSetup);
					rs.add(lcaResult);
				} catch (Exception e) {
					error = Res.error(
							"Calculation of system failed: " + b.system().name, e);
					break;
				}
			}

			if (error != null)
				break;

			result.append(simState, rs);
			progress.worked(1);
		}
	}

	private Res<List<ParameterRedef>> paramsOf(
			SimulationState simState, SystemBinding binding) {

		var params = new ArrayList<ParameterRedef>();
		for (var vb : binding.varBindings()) {
			if (vb.varId() == null || vb.parameter() == null)
				continue;
			var cell = simState.valueOf(vb.varId()).orElse(null);
			if (cell == null)
				return Res.error("Variable not found: " + vb.varId());
			if (!(cell instanceof

			NumCell(double num))) {
				return Res.error(
						"Variable does not evaluate to a number: " + vb.varId());
			}

			var param = vb.parameter().copy();
			param.value = num;
			params.add(param);
		}
		return Res.ok(params);
	}
}
