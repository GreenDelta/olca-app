package org.openlca.app.results.analysis.groups;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.agroups.AnalysisGroupResult;

record ImpactGroupResult(
		ImpactDescriptor impact,
		Map<String, Double> values,
		double max
) {

	static List<ImpactGroupResult> allOf(
			List<ImpactDescriptor> indicators, AnalysisGroupResult result
	) {
		var list = new ArrayList<ImpactGroupResult>(indicators.size());
		for (var i : indicators) {
			var values = result.getResultsOf(i);
			if (values.isEmpty())
				continue;
			double max = 0;
			for (var v : values.values()) {
				if (v == null)
					continue;
				max = Math.max(max, Math.abs(v));
			}
			list.add(new ImpactGroupResult(i, values, max));
		}
		return list;
	}

}
