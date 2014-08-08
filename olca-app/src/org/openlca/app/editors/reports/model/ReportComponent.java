package org.openlca.app.editors.reports.model;

import java.util.Objects;

public enum ReportComponent {

	NONE("none"),

	/** Name and description of the project variants */
	VARIANT_DESCRIPTION_TABLE("variant_description_table"),

	/** Name and description of the LCIA categories of the projects' method. */
	INDICATOR_DESCRIPTION_TABLE("indicator_description_table"),

	/** Name and description of the project parameters. */
	PARAMETER_DESCRIPTION_TABLE("parameter_description_table"),

	/** Parameter values for all project variants in the report. */
	PARAMETER_VALUE_TABLE("parameter_value_table"),

	/** LCIA category results for all variants in the project. */
	IMPACT_RESULT_TABLE("impact_result_table"),

	/** Normalized LCIA category results for all variants in the project. */
	NORMALISATION_RESULT_TABLE("normalisation_result_table"),

	/** Table with the single score results. */
	SINGLE_SCORE_TABLE("single_score_table"),

	/** Contributions of the processes in the variants to the LCIA results. */
	PROCESS_CONTRIBUTION_CHARTS("process_contribution_charts"),

	RESULT_CHART("total_result_chart");

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
