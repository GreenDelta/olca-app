package org.openlca.app.editors.projects.reports.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openlca.app.editors.projects.ProjectResultData;

class ReportImpactResult {

	final String indicatorId;
	final List<VariantResult> variantResults = new ArrayList<>();

	private ReportImpactResult(String indicatorId) {
		this.indicatorId = indicatorId;
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
	}

}
