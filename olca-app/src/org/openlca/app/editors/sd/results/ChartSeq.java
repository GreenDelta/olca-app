package org.openlca.app.editors.sd.results;

import java.util.ArrayList;
import java.util.List;

import org.openlca.app.editors.sd.interop.CoupledResult;
import org.openlca.sd.eqn.Subscript;
import org.openlca.sd.eqn.Tensor;
import org.openlca.sd.eqn.Var;
import org.openlca.sd.eqn.cells.NumCell;
import org.openlca.sd.eqn.cells.TensorCell;
import org.openlca.sd.util.Tensors;

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

		// it is possible (in Stella) that a stock starts with a
		// numeric value, but then arrays are added or subtracted
		// via flows and the value turns into an array then, this
		// is why we check the type of the last value
		return switch(values.getLast()) {
			case NumCell ignored -> numSeqOf(v, n);
			case TensorCell(Tensor t) -> tensorSeqsOf(v, n, t);
			case null, default -> {
				var seq = new ChartSeq(v.name().value(), new double[r.size()]);
				yield List.of(seq);
			}
		};
	}

	private static List<ChartSeq> numSeqOf(Var v, int n) {
		var values = v.values();
		var nums = new double[n];
		for (int i = 0; i < n; i++) {
			if (values.get(i) instanceof NumCell(double num)) {
				nums[i] = num;
			}
		}
		var seq = new ChartSeq(v.name().value(), nums);
		return List.of(seq);
	}

	private static List<ChartSeq> tensorSeqsOf(Var v, int n, Tensor t) {
		var addresses = Tensors.addressesOf(t);
		var cSeqs = new ArrayList<ChartSeq>(addresses.size());
		var tSeqs = new ArrayList<TensorSeq>(addresses.size());
		for (var address : addresses) {
			var seq = new ChartSeq(
					Tensors.addressKeyOf(v, address),
					new double[n]);
			cSeqs.add(seq);
			tSeqs.add(new TensorSeq(address, seq));
		}

		var values = v.values();
		for (int i = 0; i < n; i++) {
			var val = values.get(i);
			if (val instanceof NumCell(double num)) {
				for (var seq : cSeqs) {
					seq.values[i] = num;
				}
			}
			if (val instanceof TensorCell(Tensor tensor)) {
				for (var seq : tSeqs) {
					if (tensor.get(seq.address) instanceof NumCell(double num)) {
						seq.chartSeq.values[i] = num;
					}
				}
			}
		}
		return cSeqs;
	}

	private record TensorSeq(List<Subscript> address, ChartSeq chartSeq) {
	}
}
