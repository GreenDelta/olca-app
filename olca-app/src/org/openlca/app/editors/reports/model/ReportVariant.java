package org.openlca.app.editors.reports.model;

/**
 * The information of a project variant for a report. The name of the report
 * variant is the same as for the respective project variant.
 */
public class ReportVariant {

	private final int id;
	private String name;
	private String description;

	public ReportVariant(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
