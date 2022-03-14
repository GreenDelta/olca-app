package org.openlca.app.editors.projects.reports.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Stream;

import org.openlca.app.editors.projects.ProjectResultData;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.jsonld.Json;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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


	public static Optional<Report> fromFile(File reportFile, IDatabase db) {
		var obj = Json.readObject(reportFile).orElse(null);
		if (obj == null)
			return Optional.empty();

		var log = LoggerFactory.getLogger(Report.class);
		var report = new Report();

		// title & nw settings
		report.title = Json.getString(obj, "title");
		if (report.title == null) {
			log.warn("No report title could be found.");
		}
		report.withNormalisation = Json.getBool(obj, "withNormalisation", false);
		report.withWeighting = Json.getBool(obj, "withWeighting", false);

		// sections
		var sections = Json.getArray(obj, "sections");
		if (sections == null) {
			log.warn("No report sections could be found.");
		} else {
			map(sections, ReportSection::fromJson)
				.forEach(report.sections::add);
		}

		// sync processes with db
		var processes = Json.getArray(obj, "processes");
		if (processes != null) {
			var dao = new ProcessDao(db);
			map(processes, o -> {
				var id = Json.getString(o, "@id");
				return id != null
					? dao.getDescriptorForRefId(id)
					: null;
			}).forEach(report.processes::add);
		}

		var variants = Json.getArray(obj, "variants");
		if (variants == null) {
			log.warn("No report's variants could be found.");
		} else {
			map(variants, ReportVariant::fromJson)
				.forEach(report.variants::add);
		}

		var parameters = Json.getArray(obj, "parameters");
		if (parameters != null) {
			map(parameters, ReportParameter::fromJson)
				.forEach(report.parameters::add);
		}

		var indicators = Json.getArray(obj, "indicators");
		if (indicators == null) {
			log.warn("No report's indicators could be found.");
		} else {
			map(indicators, ReportIndicator::fromJson)
				.forEach(report.indicators::add);
		}

		var results = Json.getArray(obj, "results");
		if (results == null) {
			log.warn("No report's results could be found.");
		} else {
			map(results, ReportImpactResult::fromJson)
				.forEach(report.results::add);
		}

		var addedValues = Json.getArray(obj, "addedValues");
		if (addedValues != null) {
			map(addedValues, ReportCostResult::fromJson)
				.forEach(report.addedValues::add);
		}

		var netCosts = Json.getArray(obj, "netCosts");
		if (netCosts != null) {
			map(netCosts, ReportCostResult::fromJson)
				.forEach(report.netCosts::add);
		}

		return Optional.of(report);
	}

	private static <T> Stream<T> map(JsonArray array, Function<JsonObject, T> fn) {
		if (array == null || fn == null)
			return Stream.empty();
		return Json.stream(array)
			.filter(JsonElement::isJsonObject)
			.map(JsonElement::getAsJsonObject)
			.map(fn)
			.filter(Objects::nonNull);
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
		section.text = """
			This is a default template for the report of a project result. You can
			modify this template in the report editor by

			<p><ul>
			<li>changing the text of the sections,
			<li>adding or removing sections,
			<li>moving sections around,
			<li>and selecting visual components that should be shown in the sections.
			</ul></p>

			Note that you can also use HTML elements to format the section texts.
			Additionally, you can export this report as an HTML page using the export
			button in the toolbar of the report view.""";
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
