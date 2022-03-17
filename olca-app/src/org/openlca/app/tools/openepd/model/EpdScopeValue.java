package org.openlca.app.tools.openepd.model;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public record EpdScopeValue(String scope, EpdMeasurement value) {

	public static JsonObject toJson(List<EpdScopeValue> values) {
		var json = new JsonObject();
		if (values == null)
			return json;
		for (var v : values) {
			if (v.scope == null || v.value == null)
				continue;
			json.add(v.scope, v.value.toJson());
		}
		return json;
	}

	public static List<EpdScopeValue> fromJson(JsonElement elem) {
		var values = new ArrayList<EpdScopeValue>();
		if (elem == null || !elem.isJsonObject())
			return values;
		var json = elem.getAsJsonObject();
		for (var scope : json.keySet() ) {
			var m = json.get(scope);
			if (m == null)
				continue;
			var measurement = EpdMeasurement.fromJson(m);
			if (measurement.isEmpty())
				continue;
			values.add(new EpdScopeValue(scope, measurement.get()));
		}
		return values;
	}

}
