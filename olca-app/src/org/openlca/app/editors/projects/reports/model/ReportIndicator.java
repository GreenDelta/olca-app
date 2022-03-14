package org.openlca.app.editors.projects.reports.model;

import java.util.Arrays;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.openlca.core.model.Project;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.jsonld.Json;
import org.slf4j.LoggerFactory;

class ReportIndicator {

  final ImpactDescriptor impact;
  final Double normalisationFactor;
  final Double weightingFactor;

  ReportIndicator(Project project, ImpactDescriptor impact) {
    this.impact = Objects.requireNonNull(impact);
    Double nf = null;
    Double wf = null;
    if (project != null && project.nwSet != null) {
      var f = project.nwSet.getFactor(impact);
      if (f != null) {
        nf = f.normalisationFactor;
        wf = f.weightingFactor;
      }
    }
    normalisationFactor = nf;
    weightingFactor = wf;
  }

	ReportIndicator(ImpactDescriptor impact,
									Double normalisationFactor,
									Double weightingFactor) {
		this.impact = impact;
		this.normalisationFactor = normalisationFactor;
		this.weightingFactor = weightingFactor;
	}

	static ReportIndicator fromJson(JsonObject obj) {
		if (obj == null)
			return null;

		var log = LoggerFactory.getLogger(ReportIndicator.class);

		// Make sure the report is not of an old version.
		for (String constructorFieldName
			: Arrays.asList("impact", "normalisationFactor", "weightingFactor")) {
			if (!(obj.has(constructorFieldName))) {
				log.warn("Failed to parse the {} of the indicator of the report.",
					constructorFieldName);
				return null;
			}
		}

		var impact = new Gson().fromJson(obj.get("impact"), ImpactDescriptor.class);
		var normalisationFactor = Json.getDouble(obj, "normalisationFactor", 0);
		var weightingFactor = Json.getDouble(obj, "weightingFactor", 0);

		return new ReportIndicator(impact, normalisationFactor, weightingFactor);
	}

		static ReportIndicator of (Project project, ImpactDescriptor impact) {
    return new ReportIndicator(project, impact);
  }

}
