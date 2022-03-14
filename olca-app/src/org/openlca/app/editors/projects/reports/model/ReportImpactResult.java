package org.openlca.app.editors.projects.reports.model;

import java.lang.reflect.Type;
import java.util.*;

import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.openlca.app.editors.projects.ProjectResultData;
import org.openlca.jsonld.Json;
import org.slf4j.LoggerFactory;

class ReportImpactResult {

	final String indicatorId;
	final List<VariantResult> variantResults = new ArrayList<>();

	private ReportImpactResult(String indicatorId) {
		this.indicatorId = indicatorId;
	}

	static ReportImpactResult fromJson(JsonObject obj) {
		if (obj == null)
			return null;

		var log = LoggerFactory.getLogger(ReportImpactResult.class);

		var indicatorId = Json.getString(obj, "indicatorId");
		if (indicatorId == null) {
			log.warn("Failed to parse an impact result of the report.");
			return null;
		}

		ReportImpactResult reportImpactResult = new ReportImpactResult(indicatorId);

		var variantResults = Json.getArray(obj, "variantResults");
		if (variantResults == null) {
			log.warn("Failed to parse the variant's results of the impact results " +
				"of the report.");
		} else {
			Json.stream(variantResults)
				.filter(JsonElement::isJsonObject)
				.map(JsonElement::getAsJsonObject)
				.map(VariantResult::fromJson)
				.filter(Objects::nonNull)
				.forEach(reportImpactResult.variantResults::add);
		}

		return reportImpactResult;
	}

	static List<ReportImpactResult> allOf(Report report, ProjectResultData data) {
	  if (report == null || isEmpty(data))
	    return Collections.emptyList();
	  var projectResult = data.result();
	  var results = new ArrayList<ReportImpactResult>();
	  for (var impact : data.items().impacts()) {
	    var r = new ReportImpactResult(impact.refId);
	    results.add(r);
      for (var variant : data.project().variants) {
        var result = projectResult.getResult(variant);
        if (result == null)
          continue;
        var total = result.getTotalImpactResult(impact);
        var vr = new VariantResult(variant.name, total);
        r.variantResults.add(vr);
        for (var process : report.processes) {
          var contribution = result.getDirectImpactResult(process, impact);
          vr.contributions.put(process.refId, contribution);
        }
      }
    }
	  return results;
  }

  private static boolean isEmpty(ProjectResultData data) {
	  return data == null
      || data.project() == null
      || data.result() == null
      || data.items() == null
      || data.items().impacts().isEmpty();
  }

	static class VariantResult {
		final String variant;
		final double totalAmount;

		final Map<String, Double> contributions = new HashMap<>();

		private VariantResult(String variant, double totalAmount) {
		  this.variant = variant;
		  this.totalAmount = totalAmount;
    }

		static VariantResult fromJson(JsonObject obj) {
			if (obj == null)
				return null;

			var variantResult = new VariantResult(
				Json.getString(obj, "variant"),
				Json.getDouble(obj, "totalAmount", 0)
			);


			if (!obj.get("contributions").isJsonArray()) {
				Type contributionsMapType = new TypeToken<Map<String, Double>>() {}.getType();
				Map<String, Double> contributionsMap = new Gson().fromJson(obj.get("contributions"), contributionsMapType);
				variantResult.contributions.putAll(contributionsMap);
			}
			return variantResult;
		}
	}
}
