package org.openlca.app.tools.params;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.LcaResult;

record ParamResult(
		ProductSystem system,
		ImpactMethod method,
		AllocationMethod allocation,
		ParamSeq seq,
		Map<ImpactDescriptor, List<Double>> results
) {

	void append(LcaResult r) {
		var impacts = r.impactIndex();
		if (impacts == null || impacts.isEmpty())
			return;
		for (var impact : impacts) {
			double v = r.getTotalImpactValueOf(impact);
			results.computeIfAbsent(impact, i -> new ArrayList<>()).add(v);
		}
	}

	int count() {
		return seq().count();
	}

	List<ImpactDescriptor> impacts() {
		return new ArrayList<>(results.keySet());
	}

	double[] seriesOf(ImpactDescriptor d) {
		if (d == null || results == null)
			return new double[0];
		var list = results.get(d);
		if (list == null)
			return new double[0];
		return list.stream()
				.mapToDouble(Double::doubleValue)
				.toArray();
	}
}
