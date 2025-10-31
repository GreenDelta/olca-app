package org.openlca.app.editors.processes;


import java.util.HashSet;
import java.util.Set;

import org.openlca.app.db.Database;
import org.openlca.app.db.Libraries;
import org.openlca.core.database.IDatabase;
import org.openlca.core.math.ReferenceAmount;
import org.openlca.core.matrix.Demand;
import org.openlca.core.matrix.ImpactBuilder;
import org.openlca.core.matrix.MatrixData;
import org.openlca.core.matrix.ParameterTable;
import org.openlca.core.matrix.format.JavaMatrix;
import org.openlca.core.matrix.format.Matrix;
import org.openlca.core.matrix.format.MatrixBuilder;
import org.openlca.core.matrix.format.MatrixReader;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.matrix.index.EnviIndex;
import org.openlca.core.matrix.index.ImpactIndex;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.matrix.index.TechIndex;
import org.openlca.core.matrix.solvers.JavaSolver;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.results.LcaResult;
import org.openlca.core.results.providers.InversionResult;
import org.openlca.core.results.providers.LibImpactMatrix;
import org.openlca.expressions.FormulaInterpreter;
import org.openlca.util.Exchanges;
import org.openlca.util.Res;
import org.openlca.util.Strings;
import org.slf4j.LoggerFactory;

class DirectProcessResult {

	private final IDatabase db;
	private final Process process;
	private final boolean regionalized;

	private final ImpactIndex impactIdx;
	private final FormulaInterpreter interpreter;

	private DirectProcessResult(
			IDatabase db, Process process, boolean regionalized
	) {
		this.db = db;
		this.process = process;
		this.regionalized = regionalized;
		impactIdx = ImpactIndex.of(db);

		// initialize the parameter interpreter
		var iCtx = new HashSet<Long>();
		iCtx.add(process.id);
		for (var i : impactIdx) {
			iCtx.add(i.id);
		}
		interpreter = ParameterTable.interpreter(db, iCtx, Set.of());
		var scope = interpreter.getOrCreate(process.id);
		for (var param : process.parameters) {
			if (param.isInputParameter || Strings.isBlank(param.formula)) {
				scope.bind(param.name, param.value);
			} else {
				scope.bind(param.name, param.formula);
			}
		}
	}

	static Res<LcaResult> calculate(Process process, boolean regionalized) {

		if (process == null)
			return Res.error("The process is empty.");
		var qRef = process.quantitativeReference;
		if (!Exchanges.isProviderFlow(qRef))
			return Res.error(
					"The process does not have a valid quantitative reference.");

		var db = Database.get();
		if (db == null)
			return Res.error("No database is open.");

		return new DirectProcessResult(db, process, regionalized).calculate();
	}

	private Res<LcaResult> calculate() {
		try {
			var data = initMatrixData();
			if (impactIdx.isEmpty())
				return solve(data);
			var enviData = buildEnviData();
			if (enviData.isEmpty())
				return solve(data);
			enviData.setTo(data);

			var cfs = impactMatrixOf(enviData.index);
			if (cfs.hasError())
				return cfs.castError();
			data.impactIndex = impactIdx;
			data.impactMatrix = cfs.value();

			return solve(data);
		} catch (Exception e) {
			return Res.error("Calculation failed", e);
		}
	}

	private MatrixData initMatrixData() {
		// a demand of 1 and an output of 1 => so everything is scaled by 1
		// no need to put the real reference amount into the slots
		var data = new MatrixData();
		var refProduct = TechFlow.of(process);
		data.techIndex = new TechIndex(refProduct);
		data.demand = Demand.of(refProduct, 1.0);
		data.techMatrix = JavaMatrix.of(new double[][]{{1.0}});
		return data;
	}

	private double amountOf(Exchange exchange) {
		if (exchange == null)
			return 0;
		if (Strings.isBlank(exchange.formula))
			return ReferenceAmount.get(exchange);
		try {
			double amount = interpreter.getOrCreate(process.id)
					.eval(exchange.formula);
			return ReferenceAmount.get(
					amount, exchange.unit, exchange.flowPropertyFactor);
		} catch (Exception e) {
			LoggerFactory.getLogger(getClass())
					.error("failed to evaluate formula: {}", exchange.formula, e);
			return ReferenceAmount.get(exchange);
		}
	}

	private EnviData buildEnviData() {
		var index = regionalized
				? EnviIndex.createRegionalized()
				: EnviIndex.create();
		var b = new MatrixBuilder();

		var procLoc = regionalized && process.location != null
				? Descriptor.of(process.location)
				: null;

		for (var e : process.exchanges) {
			if (e.flow == null || e.flow.flowType != FlowType.ELEMENTARY_FLOW)
				continue;
			var flow = Descriptor.of(e.flow);
			var loc = regionalized && e.location != null
					? Descriptor.of(e.location)
					: procLoc;

			int i = e.isInput
					? index.add(EnviFlow.inputOf(flow, loc))
					: index.add(EnviFlow.outputOf(flow, loc));
			double amount = amountOf(e);
			if (e.isInput && amount != 0) {
				amount = -amount;
			}
			b.add(i, 0, amount);
		}

		var matrix = index.isEmpty() ? null : b.finish();
		return new EnviData(index, matrix);
	}

	private Res<MatrixReader> impactMatrixOf(EnviIndex enviIdx) {
		if (enviIdx == null || enviIdx.isEmpty())
			return Res.error("No intervention flows.");
		if (impactIdx == null || impactIdx.isEmpty())
			return Res.error("No impact categories.");

		boolean withLibs = false;
		for (var i : impactIdx) {
			if (i.isFromLibrary()) {
				withLibs = true;
				break;
			}
		}

		if (withLibs) {
			var readers = Libraries.readersForCalculation().orElse(null);
			if (readers == null)
				return Res.error("Failed to load LCIA libraries");
			var matrix = LibImpactMatrix.of(impactIdx, enviIdx).build(db, readers);
			return Res.of(matrix);
		}

		var matrix = ImpactBuilder.of(db, enviIdx)
				.withImpacts(impactIdx)
				.withInterpreter(interpreter)
				.build()
				.impactMatrix;
		return Res.of(matrix);
	}

	private Res<LcaResult> solve(MatrixData data) {
		try {
			var p = InversionResult.of(new JavaSolver(), data)
					.calculate()
					.provider();
			return Res.of(new LcaResult(p));
		} catch (Exception e) {
			return Res.error("Calculation failed", e);
		}
	}

	private record EnviData(EnviIndex index, Matrix matrix) {

		boolean isEmpty() {
			return index == null || index.isEmpty() || matrix == null;
		}

		void setTo(MatrixData data) {
			if (data == null)
				return;
			data.enviIndex = index;
			data.enviMatrix = matrix;
		}
	}
}
