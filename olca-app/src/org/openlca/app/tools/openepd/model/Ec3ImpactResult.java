package org.openlca.app.tools.openepd.model;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public record Ec3ImpactResult(
	String method, List<Ec3IndicatorResult> indicatorResults) {

	public static Ec3ImpactResult of(String method) {
		return new Ec3ImpactResult(method, new ArrayList<>());
	}

	public static JsonObject toJson(List<Ec3ImpactResult> results) {
		var json = new JsonObject();
		if (results == null)
			return json;
		for (var result : results) {
			if (result.method == null
				|| result.indicatorResults == null
				|| result.indicatorResults.isEmpty())
				continue;
			var indicators = Ec3IndicatorResult.toJson(result.indicatorResults);
			json.add(result.method, indicators);
		}
		return json;
	}

	public static List<Ec3ImpactResult> fromJson(JsonElement elem) {
		var results = new ArrayList<Ec3ImpactResult>();
		if (elem == null || !elem.isJsonObject())
			return results;
		var json = elem.getAsJsonObject();
		for (var method : json.keySet()) {
			var v = json.get(method);
			if (v == null)
				continue;
			var indicatorResults = Ec3IndicatorResult.fromJson(v);
			if (indicatorResults.isEmpty())
				continue;
			results.add(new Ec3ImpactResult(method, indicatorResults));
		}
		return results;
	}

}
