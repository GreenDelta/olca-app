package org.openlca.app.editors.projects.reports.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.google.gson.GsonBuilder;
import org.openlca.app.editors.projects.results.ProjectResultData;
import org.openlca.core.model.descriptors.ProcessDescriptor;
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
        section.text = props.getProperty(component.name() + ".text");
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

}
