package org.openlca.app.results.analysis.groups;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.openlca.commons.Strings;
import org.openlca.core.model.AnalysisGroup;

record GroupValue(AnalysisGroup group, double value, double share) {

	String name() {
		return group.name;
	}

	static List<GroupValue> allOf(
			List<AnalysisGroup> groups, Map<String, Double> map
	) {

		// select the maximum value to calculate the shares
		var items = new ArrayList<GroupValue>(groups.size());
		double max = 0;
		var visited = new HashSet<String>();
		for (var g : groups) {
			if (Strings.isBlank(g.name))
				continue;
			visited.add(g.name);
			var val = map.get(g.name);
			if (val == null)
				continue;
			max = Math.max(max, Math.abs(val));
		}

		// calculate the "rest"
		double rest = 0;
		for (var e : map.entrySet()) {
			if (visited.contains(e.getKey())
					|| e.getValue() == null)
				continue;
			rest += e.getValue();
		}

		// create the group items
		for (var g : groups) {
			var value = map.get(g.name);
			double v = value != null ? value : 0d;
			double share = max > 0 ? v / max : 0d;
			items.add(new GroupValue(g, v, share));
		}

		// add the "rest" item
		rest = Math.abs(rest) > 1e-12 ? rest : 0;
		var g = new AnalysisGroup();
		g.name = "Rest";
		double share = max > 0 ? rest / max : 0d;
		items.add(new GroupValue(g, rest, share));

		return items;
	}
}
