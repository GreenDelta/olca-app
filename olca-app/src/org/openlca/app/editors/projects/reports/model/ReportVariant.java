package org.openlca.app.editors.projects.reports.model;

import com.google.gson.JsonObject;
import org.openlca.core.model.ProjectVariant;
import org.openlca.jsonld.Json;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * The information of a project variant for a report. The name of the report
 * variant is the same as for the respective project variant.
 */
class ReportVariant {

  final String name;
  final String description;

  private ReportVariant(String name, String description) {
    this.name = name;
    this.description = description;
  }

	static ReportVariant fromJson(JsonObject obj) {
		if (obj == null)
			return null;

		var log = LoggerFactory.getLogger(ReportVariant.class);

		// Make sure the report is not of an old version.
		for (String constructorFieldName : Arrays.asList("name", "description")) {
			if (!(obj.has(constructorFieldName))) {
				log.warn("Failed to parse the {} of a variant of the report.",
					constructorFieldName);
				return null;
			}
		}

		return new ReportVariant(
			Json.getString(obj, "name"),
			Json.getString(obj, "description")
			);
	}

		static ReportVariant of(ProjectVariant v) {
    if (v == null)
      return new ReportVariant("", "");
    var name = v.name == null ? "" : v.name;
    var description = v.description == null ? "" : v.description;
    return new ReportVariant(name, description);
  }

}
