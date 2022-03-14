package org.openlca.app.editors.projects.reports.model;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.openlca.app.editors.projects.ProjectResultData;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.jsonld.Json;
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


	public static Report fromJsonFile(File reportFile)
		throws IOException {
		try (InputStream stream = new FileInputStream(reportFile);
				 Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
			JsonElement json = JsonParser.parseReader(reader);
			JsonObject obj = json.getAsJsonObject();
			Report report = new Report();

			var log = LoggerFactory.getLogger(Report.class);

			var title = Json.getString(obj, "title");
			if (title == null) {
				log.warn("No report's title could be found.");
			} else {
				report.title = title;
			}

			report.withNormalisation = Json.getBool(obj, "withNormalisation", false);
			report.withWeighting = Json.getBool(obj, "withWeighting", false);

			var sections = Json.getArray(obj, "sections");
			if (sections == null) {
				log.warn("No report's sections could be found.");
			} else {
				Json.stream(sections)
					.filter(JsonElement::isJsonObject)
					.map(JsonElement::getAsJsonObject)
					.map(ReportSection::fromJson)
					.filter(Objects::nonNull)
					.forEach(report.sections::add);
			}

			// TODO (M) Synchronize with the database.
			var processes = Json.getArray(obj, "processes");
			if (processes == null) {
				log.warn("No report's processes could be found.");
			} else {
				Json.stream(processes)
					.filter(JsonElement::isJsonObject)
					.map(JsonElement::getAsJsonObject)
					.map(elem -> new Gson().fromJson(elem, ProcessDescriptor.class))
					.filter(Objects::nonNull)
					.forEach(report.processes::add);
			}

			var variants = Json.getArray(obj, "variants");
			if (variants == null) {
				log.warn("No report's variants could be found.");
			} else {
				Json.stream(variants)
					.filter(JsonElement::isJsonObject)
					.map(JsonElement::getAsJsonObject)
					.map(ReportVariant::fromJson)
					.filter(Objects::nonNull)
					.forEach(report.variants::add);
			}

			var parameters = Json.getArray(obj, "parameters");
			if (parameters == null) {
				log.warn("No report's parameters could be found.");
			} else {
				Json.stream(parameters)
					.filter(JsonElement::isJsonObject)
					.map(JsonElement::getAsJsonObject)
					.map(ReportParameter::fromJson)
					.filter(Objects::nonNull)
					.forEach(report.parameters::add);
			}

			var indicators = Json.getArray(obj, "indicators");
			if (indicators == null) {
				log.warn("No report's indicators could be found.");
			} else {
				Json.stream(indicators)
					.filter(JsonElement::isJsonObject)
					.map(JsonElement::getAsJsonObject)
					.map(ReportIndicator::fromJson)
					.filter(Objects::nonNull)
					.forEach(report.indicators::add);
			}

			var results = Json.getArray(obj, "results");
			if (results == null) {
				log.warn("No report's results could be found.");
			} else {
				Json.stream(results)
					.filter(JsonElement::isJsonObject)
					.map(JsonElement::getAsJsonObject)
					.map(ReportImpactResult::fromJson)
					.filter(Objects::nonNull)
					.forEach(report.results::add);
			}

			var addedValues = Json.getArray(obj, "addedValues");
			if (addedValues == null) {
				log.warn("No report's added values could be found.");
			} else {
				Json.stream(addedValues)
					.filter(JsonElement::isJsonObject)
					.map(JsonElement::getAsJsonObject)
					.map(ReportCostResult::fromJson)
					.filter(Objects::nonNull)
					.forEach(report.addedValues::add);
			}

			var netCosts = Json.getArray(obj, "netCosts");
			if (netCosts == null) {
				log.warn("No report's net costs could be found.");
			} else {
				Json.stream(netCosts)
					.filter(JsonElement::isJsonObject)
					.map(JsonElement::getAsJsonObject)
					.map(ReportCostResult::fromJson)
					.filter(Objects::nonNull)
					.forEach(report.netCosts::add);
			}

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
