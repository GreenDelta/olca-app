package org.openlca.app.results.analysis.groups;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openlca.core.model.AnalysisGroup;

record GroupValue(AnalysisGroup group, double value, double share) {

	String name() {
		return group.name;
	}

	static List<GroupValue> allOf(
			List<AnalysisGroup> groups, Map<String, Double> map
	) {
		var items = new ArrayList<GroupValue>(groups.size());
		double max = 0;
		for (var g : groups) {
			var val = map.get(g.name);
			if (val == null)
				continue;
			max = Math.max(max, Math.abs(val));
		}

		for (var g : groups) {
			var value = map.get(g.name);
			double v = value != null ? value : 0d;
			double share = max > 0 ? v / max : 0d;
			items.add(new GroupValue(g, v, share));
		}
		return items;
	}
}
