package org.openlca.app.editors.reports.model;

import java.util.Objects;

public enum ReportComponent {

	NONE("none"),

	VARIANT_TABLE("variant_table"),

	INDICATOR_TABLE("indicator_table"),

	PARAMETER_TABLE("parameter_table"),

	RESULT_TABLE("result_table"),

	RESULT_CHART("total_result_chart"),

	CONTRIBUTION_CHARTS("variants_result_charts");

	private final String id;

	private ReportComponent(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public static ReportComponent getForId(String id) {
		if (id == null)
			return NONE;
		for (ReportComponent c : values()) {
			if (Objects.equals(id, c.getId()))
				return c;
		}
		return NONE;
	}

}
