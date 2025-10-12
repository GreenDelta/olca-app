package org.openlca.app.editors.sd.results;

import java.util.List;

import org.openlca.app.editors.sd.interop.CoupledResult;
import org.openlca.sd.eqn.Var;
import org.openlca.sd.eqn.cells.NumCell;

record ChartSeq(String title, double[] values) {

	static List<ChartSeq> of(CoupledResult r, Var v) {
		if (r == null || v == null)
			return List.of();

		var values = v.values();
		if (values.isEmpty()) {
			var seq = new ChartSeq(v.name().value(), new double[r.size()]);
			return List.of(seq);
		}

		int n = Math.min(values.size(), r.size());
		var first = values.getFirst();
		if (first instanceof NumCell) {

		}

	}
}
