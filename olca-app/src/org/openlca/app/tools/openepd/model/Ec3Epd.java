package org.openlca.app.tools.openepd.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.openlca.jsonld.Json;

public class Ec3Epd {

  public String id;
  public String name;
  public String description;
	public Ec3Category category;

  public String declaredUnit;
  public String massPerDeclaredUnit;
  public String gwp;
  public String gwpPerKg;

  public Ec3Certifier reviewer;
  public Ec3Certifier developer;
  public Ec3Certifier verifier;
	public Ec3Org manufacturer;

  public String docUrl;

	public boolean isDraft;
	public boolean isPrivate;

	private final Map<String, Ec3ImpactSet> impacts = new HashMap<>();
	private final List<Ec3ImpactResult> impactResults = new ArrayList<>();

  public static Optional<Ec3Epd> fromJson(JsonElement elem) {
    if (elem == null || !elem.isJsonObject())
      return Optional.empty();
    var obj = elem.getAsJsonObject();
    var epd = new Ec3Epd();
    epd.id = Json.getString(obj, "id");
    epd.name = Json.getString(obj, "name");
    epd.description = Json.getString(obj, "description");
		epd.category = Ec3Category.fromJson(
			obj.get("category")).orElse(null);

    epd.declaredUnit = Json.getString(obj, "declared_unit");
    epd.massPerDeclaredUnit = Json.getString(obj, "mass_per_declared_unit");
    epd.gwp = Json.getString(obj, "gwp");
    epd.gwpPerKg = Json.getString(obj, "gwp_per_kg");

    epd.reviewer = Ec3Certifier.fromJson(obj.get("reviewer")).orElse(null);
    epd.developer = Ec3Certifier.fromJson(obj.get("developer")).orElse(null);
    epd.verifier = Ec3Certifier.fromJson(obj.get("verifier")).orElse(null);
		epd.manufacturer = Ec3Org.fromJson(obj.get("manufacturer")).orElse(null);

    epd.docUrl = Json.getString(obj, "doc");
		epd.isDraft = Json.getBool(obj, "draft", false);
		epd.isPrivate = Json.getBool(obj, "private", false);


		// impacts
		var impactJson = Json.getObject(obj, "impacts");
		if (impactJson != null) {
			for (var method : impactJson.keySet()) {
				var result = Ec3ImpactResult.of(method);
				var indicatorJson = Json.getObject(impactJson, method);
				if (indicatorJson == null)
					continue;
				for (var indicator : indicatorJson.keySet()) {
					var indicatorResult = Ec3IndicatorResult.of(indicator);
					var scopeJson = Json.getObject(indicatorJson, indicator);
					if (scopeJson == null)
						continue;
					for (var scope : )
				}
				if (!indicatorJson.isEmpty()) {
					epd.putImpactSet(method, indicatorJson);
				}
			}
		}

		return Optional.of(epd);
  }

  public JsonObject toJson() {
    var obj = new JsonObject();

    Json.put(obj, "id", id);
    Json.put(obj, "name", name);
    Json.put(obj, "description", description);

    Json.put(obj, "declared_unit", declaredUnit);
    Json.put(obj, "mass_per_declared_unit", massPerDeclaredUnit);
    Json.put(obj, "gwp", gwp);
    Json.put(obj, "gwp_per_kg", gwpPerKg);
		if (category != null) {
			obj.add("category", category.toJson());
		}

    Json.put(obj, "doc", docUrl);

		obj.addProperty("draft", isDraft);
		obj.addProperty("private", isPrivate);

    if (reviewer != null) {
      obj.add("reviewer", reviewer.toJson());
    }
    if (developer != null) {
      obj.add("developer", developer.toJson());
    }
    if (verifier != null) {
      obj.add("verifier", verifier.toJson());
    }

		// impacts
		var impactObj = new JsonObject();
		eachImpactSet((method, impactSet) -> {
			if (method != null && impactSet != null) {
				impactObj.add(method, impactSet.toJson());
			}
		});
		if (impactObj.size() > 0) {
			obj.add("impacts", impactObj);
		}

		// impact results
		var impactJson = new JsonObject();
		for (var result : impactResults) {
			var indicatorJson = new JsonObject();
			for (var indicatorResult : result.indicatorResults()) {
				var scopeJson = new JsonObject();
				for (var scopeValue : indicatorResult.values()) {
					if (scopeValue.value() == null)
						continue;
					var measurement = scopeValue.value().toJson();
					scopeJson.add(scopeValue.scope(), measurement);
				}
				if (scopeJson.size() > 0) {
					indicatorJson.add(indicatorResult.indicator(), scopeJson);
				}
			}
			if (indicatorJson.size() > 0) {
				impactJson.add(result.method(), indicatorJson);
			}
		}
		if (impactJson.size() > 0) {
			obj.add("impacts", impactJson);
		}

    return obj;
  }

	public List<Ec3ImpactResult> impactResults() {
		return impactResults;
	}

	public Optional<Ec3ImpactSet> getImpactSet(String method) {
		return Optional.ofNullable(impacts.get(method));
	}

	public Iterable<String> impacts() {
		return Collections.unmodifiableCollection(impacts.keySet());
	}

	public void putImpactSet(String method, Ec3ImpactSet impactSet) {
		if (method == null)
			return;
		impacts.put(method, impactSet);
	}

	public void eachImpactSet(BiConsumer<String, Ec3ImpactSet> fn) {
		if (fn == null)
			return;
		for (var e : impacts.entrySet()) {
			var method = e.getKey();
			var impactSet = e.getValue();
			if (method != null && impactSet != null) {
				fn.accept(method, impactSet);
			}
		}
	}

	public void clearImpacts() {
		impacts.clear();
	}
}
