package org.openlca.app.results.analysis.groups;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openlca.core.model.CalculationSetup;
import org.openlca.core.model.ProductSystem;
import org.openlca.ilcd.util.Strings;
import org.openlca.jsonld.Json;

import com.google.gson.JsonElement;

public record AnalysisGroup(String name, Set<Long> processes) {

	public static List<AnalysisGroup> getAllOf(CalculationSetup setup) {
		if (setup == null || !(setup.target() instanceof ProductSystem sys))
			return Collections.emptyList();
		var ext = sys.readOtherProperties();
		var array = Json.getArray(ext, "analysisGroups");
		if (array == null || array.isEmpty())
			return Collections.emptyList();
		var groups = new ArrayList<AnalysisGroup>(array.size());
		for (var e : array) {
			var group = fromJson(e);
			if (group != null) {
				groups.add(group);
			}
		}
		groups.sort((g1, g2) -> Strings.compare(g1.name, g2.name));
		return groups;
	}

	private static AnalysisGroup fromJson(JsonElement elem) {
		if (elem == null || !elem.isJsonObject())
			return null;
		var obj = elem.getAsJsonObject();
		var name = Json.getString(obj, "name");
		if (Strings.nullOrEmpty(name))
			return null;
		var array = Json.getArray(obj, "processes");
		if (array == null || array.isEmpty())
			return new AnalysisGroup(name, Set.of());
		var processes = new HashSet<Long>(array.size());
		for (var e : array) {
			if (!e.isJsonPrimitive())
				continue;
			var prim = e.getAsJsonPrimitive();
			if (prim.isNumber()) {
				processes.add(prim.getAsLong());
			}
		}
		return new AnalysisGroup(name, processes);
	}

}
