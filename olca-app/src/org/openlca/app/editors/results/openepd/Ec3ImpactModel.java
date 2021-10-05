package org.openlca.app.editors.results.openepd;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.openlca.core.model.ResultImpact;
import org.openlca.core.model.ResultModel;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.ilcd.methods.ImpactModel;
import org.openlca.jsonld.Json;
import org.openlca.util.Pair;

class Ec3ImpactModel {

	final List<Ec3ImpactMethod> methods = new ArrayList<>();

	private static Ec3ImpactModel empty() {
		return new Ec3ImpactModel();
	}

	static Ec3ImpactModel read() {
		// maybe also check the workspace later
		var stream = ImpactModel.class.getResourceAsStream(
			"impact_model.json");
		if (stream == null)
			return empty();
		try (var reader = new InputStreamReader(
			stream, StandardCharsets.UTF_8)) {
			var json = new Gson().fromJson(reader, JsonObject.class);
			return fromJson(json);
		} catch (Exception e) {
			return empty();
		}
	}

	private static Ec3ImpactModel fromJson(JsonObject json) {
		if (json == null)
			return empty();
		var model = new Ec3ImpactModel();
		Json.stream(Json.getArray(json, "methods"))
			.filter(JsonElement::isJsonObject)
			.map(JsonElement::getAsJsonObject)
			.map(Ec3ImpactMethod::fromJson)
			.forEach(model.methods::add);
		return model;
	}

	Pair<Ec3ImpactMethod, ImpactMethodDescriptor> map(ImpactMethodDescriptor d) {
		if (d == null)
			return null;
		var score = 0;
		Ec3ImpactMethod selected = null;
		for (var next : methods) {
			var nextScore = mapScore(d.name, next.keywords);
			if (selected == null || nextScore > score) {
				selected = next;
				score = nextScore;
			}
		}
		return selected == null
			? null
			: Pair.of(selected, d);
	}

	private static int mapScore(String s, List<String> keywords) {
		if (s == null || keywords.isEmpty()) {
			return 0;
		}
		var feed = s.toLowerCase();
		var score = 0;
		for (var kw : keywords) {
			if (kw == null)
				continue;
			var k = kw.trim().toLowerCase();
			if (feed.contains(k)) {
				score++;
			}
		}
		return score;
	}

	static class Ec3ImpactMethod {
		String name;
		String description;
		final List<String> keywords = new ArrayList<>();
		final List<Ec3ImpactIndicator> indicators = new ArrayList<>();

		private static Ec3ImpactMethod fromJson(JsonObject json) {
			if (json == null)
				return null;

			var method = new Ec3ImpactMethod();
			method.name = Json.getString(json, "name");
			method.description = Json.getString(json, "description");
			Json.stream(Json.getArray(json, "keywords"))
				.filter(JsonElement::isJsonPrimitive)
				.map(JsonElement::getAsString)
				.forEach(method.keywords::add);

			Json.stream(Json.getArray(json, "indicators"))
				.filter(JsonElement::isJsonObject)
				.map(JsonElement::getAsJsonObject)
				.map(Ec3ImpactIndicator::fromJson)
				.filter(Objects::nonNull)
				.forEach(method.indicators::add);

			return method;
		}

		/**
		 * Maps the names (codes) of the indicators of this method
		 * to the best matching impact results of the given result
		 * (a "stable-mariage-problem").
		 */
		Map<String, ResultImpact> map(ResultModel result) {
			var unmatchedResults = new ArrayDeque<ResultImpact>();
			for (var impact : result.impacts) {
				unmatchedResults.add(impact.copy());
			}
			var scores = new HashMap<String, Pair<ResultImpact, Integer>>();

			while (!unmatchedResults.isEmpty()) {
				var impact = unmatchedResults.poll();
				if (impact.indicator == null)
					continue;

				var score = 0;
				Ec3ImpactIndicator selected = null;

				// first try the unmapped indicators
				for (var indicator : this.indicators) {
					var nextScore = mapScore(
						impact.indicator.name, indicator.keywords);
					if (selected == null || nextScore > score) {

						// checked if the indicator is already mapped
						var mapped = scores.get(indicator.name);
						if (mapped != null && mapped.second >= nextScore) {
							continue;
						}
						score = nextScore;
						selected = indicator;
					}
				}

				if (selected == null)
					continue;

				var old = scores.put(selected.name, Pair.of(impact, score));
				if (old != null) {
					unmatchedResults.add(old.first);
				}
			}

			var m = new HashMap<String, ResultImpact>();
			for (var e : scores.entrySet()) {
				m.put(e.getKey(), e.getValue().first);
			}
			return m;
		}
	}

	static class Ec3ImpactIndicator {
		String name;
		String description;
		String unit;
		List<String> keywords;

		private static Ec3ImpactIndicator fromJson(JsonObject json) {
			if (json == null)
				return null;
			var indicator = new Ec3ImpactIndicator();
			indicator.name = Json.getString(json, "name");
			indicator.description = Json.getString(json, "description");
			indicator.unit = Json.getString(json, "unit");
			Json.stream(Json.getArray(json, "keywords"))
				.filter(JsonElement::isJsonPrimitive)
				.map(JsonElement::getAsString)
				.forEach(indicator.keywords::add);
			return indicator;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof Ec3ImpactIndicator))
				return false;
			var other = (Ec3ImpactIndicator) o;
			return Objects.equals(this.name, other.name);
		}

		@Override
		public int hashCode() {
			return name == null
				? super.hashCode()
				: name.hashCode();
		}
	}
}
