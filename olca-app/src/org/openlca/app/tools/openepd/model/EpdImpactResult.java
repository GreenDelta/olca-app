package org.openlca.app.tools.openepd.model;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public record EpdImpactResult(
	String method, List<EpdIndicatorResult> indicatorResults) {

	public static EpdImpactResult of(String method) {
		return new EpdImpactResult(method, new ArrayList<>());
	}

	public static JsonObject toJson(List<EpdImpactResult> results) {
		var json = new JsonObject();
		if (results == null)
			return json;
		for (var result : results) {
			if (result.method == null
				|| result.indicatorResults == null
				|| result.indicatorResults.isEmpty())
				continue;
			var indicators = EpdIndicatorResult.toJson(result.indicatorResults);
			json.add(result.method, indicators);
		}
		return json;
	}

	public static List<EpdImpactResult> fromJson(JsonElement elem) {
		var results = new ArrayList<EpdImpactResult>();
		if (elem == null || !elem.isJsonObject())
			return results;
		var json = elem.getAsJsonObject();
		for (var method : json.keySet()) {
			var v = json.get(method);
			if (v == null)
				continue;
			var indicatorResults = EpdIndicatorResult.fromJson(v);
			if (indicatorResults.isEmpty())
				continue;
			results.add(new EpdImpactResult(method, indicatorResults));
		}
		return results;
	}

}
