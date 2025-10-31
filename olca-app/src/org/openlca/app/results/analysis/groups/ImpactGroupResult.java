package org.openlca.app.results.analysis.groups;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.openlca.core.model.AnalysisGroup;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.agroups.AnalysisGroupResult;
import org.openlca.util.Strings;

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

	double restOf(List<AnalysisGroup> groups) {
		if (groups == null || values == null)
			return 0;
		var visited = new HashSet<String>();
		for (var g : groups) {
			if (Strings.isBlank(g.name))
				continue;
			visited.add(g.name);
		}

		double rest = 0;
		for (var e : values.entrySet()) {
			if (visited.contains(e.getKey())
					|| e.getValue() == null)
				continue;
			rest += e.getValue();
		}

		return Math.abs(rest) > 1e-12 ? rest : 0;
	}
}
