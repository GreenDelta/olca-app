package org.openlca.app.editors.projects.reports.model;

import com.google.gson.JsonObject;
import org.openlca.core.model.Copyable;
import org.openlca.jsonld.Json;
import org.slf4j.LoggerFactory;


public class ReportSection implements Copyable<ReportSection> {

	public int index;
	public String title;
	public String text;
	public String componentId;

	static ReportSection fromJson(JsonObject obj) {
		if (obj == null) {
			return null;
		}

		var log = LoggerFactory.getLogger(ReportSection.class);

		var reportSection = new ReportSection();

		reportSection.index = Json.getInt(obj, "index", 0);

		var title = Json.getString(obj, "title");
		if (title == null) {
			log.warn("Failed to parse the title of a section of the report.");
		} else {
			reportSection.title = title;
		}

		var text = Json.getString(obj, "text");
		if (text == null) {
			log.warn("Failed to parse the text of a section of the report.");
		} else {
			reportSection.text = text;
		}

		reportSection.componentId = Json.getString(obj, "componentId");

		return reportSection;
	}

	@Override
	public ReportSection copy() {
		var copy = new ReportSection();
		copy.index = index;
		copy.text = text;
		copy.title = title;
		copy.componentId = componentId;
		return copy;
	}
}
