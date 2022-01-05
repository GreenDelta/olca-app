package org.openlca.app.tools.openepd.model;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public record Ec3IndicatorResult(String indicator, List<Ec3ScopeValue> values) {

	public static Ec3IndicatorResult of(String indicator) {
		return new Ec3IndicatorResult(indicator, new ArrayList<>());
	}

	public static JsonObject toJson(List<Ec3IndicatorResult> results) {
		var json = new JsonObject();
		if (results == null)
			return json;
		for (var result : results) {
			if (result.indicator == null
				|| result.values == null
				|| result.values.isEmpty())
				continue;
			var values = Ec3ScopeValue.toJson(result.values);
			json.add(result.indicator, values);
		}
		return json;
	}

	public static List<Ec3IndicatorResult> fromJson(JsonElement elem) {
		var results = new ArrayList<Ec3IndicatorResult>();
		if (elem == null || !elem.isJsonObject())
			return results;
		var json = elem.getAsJsonObject();
		for (var indicator : json.keySet()) {
			var v = json.get(indicator);
			if (v == null)
				continue;
			var values = Ec3ScopeValue.fromJson(v);
			if (values.isEmpty())
				continue;
			results.add(new Ec3IndicatorResult(indicator, values));
		}
		return results;
	}

}
