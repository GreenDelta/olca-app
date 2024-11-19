package org.openlca.app.results.analysis.groups;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openlca.core.model.AnalysisGroup;

record GroupValue(AnalysisGroup group, double value) {

	String name() {
		return group.name;
	}

	static List<GroupValue> allOf(
			List<AnalysisGroup> groups, Map<String, Double> map
	) {
		var items = new ArrayList<GroupValue>(groups.size());
		for (var g : groups) {
			var value = map.get(g.name);
			items.add(new GroupValue(g, value != null ? value : 0d));
		}
		return items;
	}
}
