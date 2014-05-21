package org.openlca.app.editors.reports;

enum ReportComponent {

	NONE("none"),

	PARAMETER_TABLE("parameter_table"),

	VARIANT_TABLE("variant_table"),

	RESULT_TABLE("result_table"),

	RESULT_CHART("result_chart");

	private final String id;

	private ReportComponent(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

}
