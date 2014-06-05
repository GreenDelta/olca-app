package org.openlca.app.editors.reports.model;

public enum ReportComponent {

	NONE("none"),

	PARAMETER_TABLE("parameter_table"),

	VARIANT_TABLE("variant_table"),

	RESULT_TABLE("result_table"),

	RESULT_CHART("result_chart"),

	CONTRIBUTION_CHARTS("contribution_charts");

	private final String id;

	private ReportComponent(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

}
