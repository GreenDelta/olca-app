package org.openlca.app.editors.projects.reports.model;

import org.openlca.core.model.ProjectVariant;

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

  static ReportVariant of(ProjectVariant v) {
    if (v == null)
      return new ReportVariant("", "");
    var name = v.name == null ? "" : v.name;
    var description = v.description == null ? "" : v.description;
    return new ReportVariant(name, description);
  }

}
