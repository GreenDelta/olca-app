package org.openlca.app.editors.results.openepd.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Ec3ScopeSet {

	private final Map<String, Ec3Measurement> entries = new HashMap<>();

	public static Ec3ScopeSet fromJson(JsonElement elem) {
		var set = new Ec3ScopeSet();
		if (elem == null || !elem.isJsonObject())
			return set;
		var obj = elem.getAsJsonObject();
		for (var prop : obj.keySet()) {
			Ec3Measurement.fromJson(obj.get(prop))
				.ifPresent(m -> set.put(prop, m));
		}
		return set;
	}

	public boolean isEmpty() {
		return entries.isEmpty();
	}

	public JsonObject toJson() {
		var obj = new JsonObject();
		each((scope, measurement) -> {
			if (scope != null && measurement != null) {
				obj.add(scope, measurement.toJson());
			}
		});
		return obj;
	}

	public void put(String scope, Ec3Measurement measurement) {
		if (scope == null)
			return;
		entries.put(scope, measurement);
	}

	public Optional<Ec3Measurement> get(String scope) {
		var m = entries.get(scope);
		return Optional.ofNullable(m);
	}

	public Iterable<String> scopes() {
		return Collections.unmodifiableCollection(entries.keySet());
	}

	public void each(BiConsumer<String, Ec3Measurement> fn) {
		if (fn == null)
			return;
		for (var e : entries.entrySet()) {
			var scope = e.getKey();
			var m = e.getValue();
			if (scope != null && m != null) {
				fn.accept(scope, m);
			}
		}
	}

}
