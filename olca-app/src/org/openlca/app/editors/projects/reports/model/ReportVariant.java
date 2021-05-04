package org.openlca.app.editors.projects.reports.model;

/**
 * The information of a project variant for a report. The name of the report
 * variant is the same as for the respective project variant.
 */
public class ReportVariant {

	public final int id;
	public String name;
	public String description;
	public boolean isDisabled;

	public ReportVariant(int id) {
		this.id = id;
	}

}
