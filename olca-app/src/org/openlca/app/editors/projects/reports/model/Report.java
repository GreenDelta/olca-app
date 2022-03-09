package org.openlca.app.editors.projects.reports.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.openlca.app.editors.projects.ProjectResultData;
import org.openlca.app.tools.openepd.model.Ec3Epd;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.jsonld.Json;
import org.openlca.app.util.ErrorReporter;
import org.slf4j.LoggerFactory;

public class Report {

	public String title;
	public boolean withNormalisation;
	public boolean withWeighting;
	public final List<ReportSection> sections = new ArrayList<>();
	public final List<ProcessDescriptor> processes = new ArrayList<>();

	final List<ReportVariant> variants = new ArrayList<>();
	final List<ReportParameter> parameters = new ArrayList<>();
	final List<ReportIndicator> indicators = new ArrayList<>();
	final List<ReportImpactResult> results = new ArrayList<>();
	final List<ReportCostResult> addedValues = new ArrayList<>();
	final List<ReportCostResult> netCosts = new ArrayList<>();

	public static Report fromJson(File reportFile)
		throws IOException {
		try (InputStream stream = new FileInputStream(reportFile);
				Reader reader = new InputStreamReader(stream, "UTF-8")) {
			JsonElement json = JsonParser.parseReader(reader);
			JsonObject obj = json.getAsJsonObject();
			Report report = new Report();
			
			
			
			return report;
		}
	}

	/**
	 * Removes the result data from this report. The result data are the
	 * dynamic data of a report that may change with every calculation.
	 */
	public void clearResults() {
		var lists = List.of(
			parameters,
			variants,
			indicators,
			results,
			addedValues,
			netCosts);
		for (var list : lists) {
			list.clear();
		}
		withNormalisation = false;
		withWeighting = false;
	}

	public Report fillWith(ProjectResultData data) {
		ReportFiller.of(data).fill(this);
		return this;
	}

	public String toJson() {
		return new GsonBuilder()
			.setPrettyPrinting()
			.create()
			.toJson(this);
	}

	/**
	 * Initializes a new report with default sections.
	 */
	public static Report initDefault() {
		var report = new Report();
		report.title = "New report";
		report.sections.add(intro());
		try {
			var props = new Properties();
			props.load(Report.class.getResourceAsStream("default_sections.properties"));
			var components = List.of(
				ReportComponent.VARIANT_DESCRIPTION_TABLE,
				ReportComponent.INDICATOR_DESCRIPTION_TABLE,
				ReportComponent.IMPACT_RESULT_TABLE,
				ReportComponent.INDICATOR_BAR_CHART,
				ReportComponent.PROCESS_CONTRIBUTION_CHART,
				ReportComponent.RELATIVE_INDICATOR_BAR_CHART
			);
			int idx = 1;
			for (var component : components) {
				var section = new ReportSection();
				section.index = idx;
				section.title = props.getProperty(component.name() + ".title");
				section.text = wrap(props.getProperty(component.name() + ".text"));
				section.componentId = component.id();
				report.sections.add(section);
				idx++;
			}
		} catch (Exception e) {
			var log = LoggerFactory.getLogger(Report.class);
			log.error("failed to create default report sections", e);
		}
		return report;
	}

	private static ReportSection intro() {
		var section = new ReportSection();
		section.index = 0;
		section.title = "Introduction";
		section.text = "This is a default template for the report of a project result. "
			+ "You can modify this template in the report editor by \n"
			+ "\n"
			+ "<p><ul>\n"
			+ "<li>changing the text of the sections, \n"
			+ "<li>adding or removing sections, \n"
			+ "<li>moving sections around, \n"
			+ "<li>and selecting visual components that should be shown in the sections. \n"
			+ "</ul></p>\n"
			+ "\n"
			+ "Note that you can also use HTML elements to format the section texts. "
			+ "Additionally, you can export this report as an HTML page using "
			+ "the export button in the toolbar of the report view.";
		return section;
	}

	private static String wrap(String s) {
		if (s == null)
			return "";
		if (s.length() <= 80)
			return s;
		var text = new StringBuilder();
		var line = new StringBuilder();
		for (var word : s.split(" ")) {
			if (line.length() + word.length() > 79) {
				text.append(line).append('\n');
				line.setLength(0);
			}
			if (line.length() > 0) {
				line.append(' ');
			}
			line.append(word);
		}
		if (line.length() > 0) {
			text.append(line);
		}
		return text.toString();
	}

}
