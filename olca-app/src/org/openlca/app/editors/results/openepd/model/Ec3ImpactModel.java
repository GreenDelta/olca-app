package org.openlca.app.editors.results.openepd.model;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ResultImpact;
import org.openlca.core.model.ResultModel;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.jsonld.Json;
import org.openlca.util.Pair;
import org.openlca.util.Strings;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public record Ec3ImpactModel(List<Method> methods, List<Indicator> indicators) {

	private static Ec3ImpactModel empty() {
		return new Ec3ImpactModel(
			Collections.emptyList(), Collections.emptyList());
	}

	public static Ec3ImpactModel get() {
		// maybe also check the workspace later
		var stream = Ec3ImpactModel.class.getResourceAsStream("impact_model.json");
		if (stream == null)
			return empty();
		try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
			var json = new Gson().fromJson(reader, JsonObject.class);
			return fromJson(json);
		} catch (Exception e) {
			return empty();
		}
	}

	private static Ec3ImpactModel fromJson(JsonObject json) {
		if (json == null)
			return empty();

		var model = new Ec3ImpactModel(new ArrayList<>(), new ArrayList<>());

		Json.stream(Json.getArray(json, "indicators"))
			.filter(JsonElement::isJsonObject)
			.map(JsonElement::getAsJsonObject)
			.map(Indicator::of)
			.forEach(model.indicators::add);

		Json.stream(Json.getArray(json, "methods"))
			.filter(JsonElement::isJsonObject)
			.map(JsonElement::getAsJsonObject)
			.map(obj -> Method.of(obj, model.indicators))
			.forEach(model.methods::add);
		model.methods().sort((m1, m2) -> Strings.compare(m1.name(), m2.name()));

		return model;
	}

	public Method getMethod(String id) {
		return methods.stream()
			.filter(m -> Objects.equals(m.id, id))
			.findAny()
			.orElse(null);
	}

	public Indicator getIndicator(String id) {
		return indicators.stream()
			.filter(i -> Objects.equals(i.id, id))
			.findAny()
			.orElse(null);
	}

	public Method match(ImpactMethodDescriptor d) {
		if (d == null)
			return null;
		var score = 0;
		Method selected = null;
		for (var next : methods) {
			var nextScore = mapScore(d.name, next.keywords());
			if (nextScore == 0)
				continue;
			if (selected == null || nextScore > score) {
				selected = next;
				score = nextScore;
			}
		}
		return selected;
	}

	public static int mapScore(String s, List<String> keywords) {
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


	public record Method(
		String id,
		String name,
		String description,
		List<String> keywords,
		List<Indicator> indicators) {

		static Method of(JsonObject json, List<Indicator> indicators) {
			if (json == null)
				return null;

			var method = new Method(
				Json.getString(json, "id"),
				Json.getString(json, "name"),
				Json.getString(json, "description"),
				new ArrayList<>(),
				new ArrayList<>()
			);

			Json.stream(Json.getArray(json, "keywords"))
				.filter(JsonElement::isJsonPrimitive)
				.map(JsonElement::getAsString)
				.forEach(method.keywords::add);

			var indicatorMap = new HashMap<String, Indicator>();
			for (var i : indicators) {
				indicatorMap.put(i.id(), i);
			}
			Json.stream(Json.getArray(json, "indicators"))
				.filter(JsonElement::isJsonPrimitive)
				.map(id -> indicatorMap.get(id.getAsString()))
				.filter(Objects::nonNull)
				.forEach(method.indicators::add);

			return method;
		}

		/**
		 * Maps the names (codes) of the indicators of this method
		 * to the best matching impact results of the given result
		 * (a "stable-marriage-problem").
		 */
		public Map<String, ResultImpact> matchIndicators(ResultModel result) {
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
				Indicator selected = null;

				// first try the unmapped indicators
				for (var indicator : this.indicators) {
					var nextScore = mapScore(
						impact.indicator.name, indicator.keywords());

					if (nextScore > score) {

						// checked if the indicator is already mapped
						var mapped = scores.get(indicator.id());
						if (mapped != null && mapped.second >= nextScore) {
							continue;
						}
						score = nextScore;
						selected = indicator;
					}
				}

				if (selected == null)
					continue;

				var old = scores.put(selected.id(), Pair.of(impact, score));
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

		/**
		 * Find the best matching impact assessment method from  the given
		 * list. Returns null if no such method could be found.
		 */
		public ImpactMethod matchMethod(List<ImpactMethod> methods) {
			if (methods == null)
				return null;
			ImpactMethod selected = null;
			var score = 0;
			for (var method : methods) {
				var nextScore = mapScore(method.name, keywords);
				if (nextScore == 0)
					continue;
				if (selected == null || nextScore > score) {
					selected = method;
					score = nextScore;
				}
			}
			return selected;
		}

	}

	public record Indicator(
		String id,
		String name,
		String description,
		String unit,
		List<String> keywords) {

		static Indicator of(JsonObject json) {
			if (json == null)
				return null;
			var indicator = new Indicator(
				Json.getString(json, "id"),
				Json.getString(json, "name"),
				Json.getString(json, "description"),
				Json.getString(json, "unit"),
				new ArrayList<>()
			);
			Json.stream(Json.getArray(json, "keywords"))
				.filter(JsonElement::isJsonPrimitive)
				.map(JsonElement::getAsString)
				.forEach(indicator.keywords::add);
			return indicator;
		}
	}
}
