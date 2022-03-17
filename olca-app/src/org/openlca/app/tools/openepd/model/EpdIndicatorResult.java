package org.openlca.app.tools.openepd.model;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public record EpdIndicatorResult(String indicator, List<EpdScopeValue> values) {

	public static EpdIndicatorResult of(String indicator) {
		return new EpdIndicatorResult(indicator, new ArrayList<>());
	}

	public static JsonObject toJson(List<EpdIndicatorResult> results) {
		var json = new JsonObject();
		if (results == null)
			return json;
		for (var result : results) {
			if (result.indicator == null
				|| result.values == null
				|| result.values.isEmpty())
				continue;
			var values = EpdScopeValue.toJson(result.values);
			json.add(result.indicator, values);
		}
		return json;
	}

	public static List<EpdIndicatorResult> fromJson(JsonElement elem) {
		var results = new ArrayList<EpdIndicatorResult>();
		if (elem == null || !elem.isJsonObject())
			return results;
		var json = elem.getAsJsonObject();
		for (var indicator : json.keySet()) {
			var v = json.get(indicator);
			if (v == null)
				continue;
			var values = EpdScopeValue.fromJson(v);
			if (values.isEmpty())
				continue;
			results.add(new EpdIndicatorResult(indicator, values));
		}
		return results;
	}

}
