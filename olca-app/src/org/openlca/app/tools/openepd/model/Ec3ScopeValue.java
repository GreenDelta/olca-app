package org.openlca.app.tools.openepd.model;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public record Ec3ScopeValue(String scope, Ec3Measurement value) {

	public static JsonObject toJson(List<Ec3ScopeValue> values) {
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

	public static List<Ec3ScopeValue> fromJson(JsonElement elem) {
		var values = new ArrayList<Ec3ScopeValue>();
		if (elem == null || !elem.isJsonObject())
			return values;
		var json = elem.getAsJsonObject();
		for (var scope : json.keySet() ) {
			var m = json.get(scope);
			if (m == null)
				continue;
			var measurement = Ec3Measurement.fromJson(m);
			if (measurement.isEmpty())
				continue;
			values.add(new Ec3ScopeValue(scope, measurement.get()));
		}
		return values;
	}

}
