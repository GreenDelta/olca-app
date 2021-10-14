package org.openlca.app.editors.results.openepd.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Ec3ImpactSet {

	private final Map<String, Ec3ScopeSet> entries = new HashMap<>();

	public static Ec3ImpactSet fromJson(JsonElement elem) {
		var set = new Ec3ImpactSet();
		if (elem == null || !elem.isJsonObject())
			return set;
		var obj = elem.getAsJsonObject();
		for (var indicator : obj.keySet()) {
			var scopes = Ec3ScopeSet.fromJson(obj.get(indicator));
			if (!scopes.isEmpty()) {
				set.put(indicator, scopes);
			}
		}
		return set;
	}

	public boolean isEmpty() {
		return entries.isEmpty();
	}

	public JsonObject toJson() {
		var obj = new JsonObject();
		each((indicator, scopes) -> obj.add(indicator, scopes.toJson()));
		return obj;
	}

	public void put(String indicator, Ec3ScopeSet scopes) {
		if (indicator == null)
			return;
		entries.put(indicator, scopes);
	}

	public Optional<Ec3ScopeSet> get(String indicator) {
		var s = entries.get(indicator);
		return Optional.ofNullable(s);
	}

	public Iterable<String> indicators() {
		return Collections.unmodifiableCollection(entries.keySet());
	}

	public void each(BiConsumer<String, Ec3ScopeSet> fn) {
		if (fn == null)
			return;
		for (var e : entries.entrySet()) {
			var indicator = e.getKey();
			var scopes = e.getValue();
			if (indicator != null && scopes != null) {
				fn.accept(indicator, scopes);
			}
		}
	}

}
