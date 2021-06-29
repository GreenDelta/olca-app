package org.openlca.app.editors.projects;

import org.eclipse.jface.viewers.LabelProvider;
import org.openlca.app.M;
import org.openlca.app.editors.projects.reports.model.ReportComponent;

class ReportComponentLabel extends LabelProvider {

	@Override
	public String getText(Object element) {
		if (!(element instanceof ReportComponent))
			return null;
		var component = (ReportComponent) element;
		return getLabel(component);
	}

	private String getLabel(ReportComponent component) {
		return switch (component) {
			case NONE -> M.None;
			case VARIANT_DESCRIPTION_TABLE -> M.VariantDescriptionTable;
			case INDICATOR_DESCRIPTION_TABLE -> M.LciaCategoryDescriptionTable;
			case PARAMETER_DESCRIPTION_TABLE -> M.ParameterDescriptionTable;
			case PARAMETER_VALUE_TABLE -> M.ParameterValueTable;
			case IMPACT_RESULT_TABLE -> M.LciaResultTable;
			case PROCESS_CONTRIBUTION_CHART -> M.ProcessContributionChart;
			case PROCESS_CONTRIBUTION_TABLE -> "Process contribution table";
			case NORMALISATION_RESULT_TABLE -> M.NormalisationResultTable;
			case SINGLE_SCORE_TABLE -> M.SingleScoreTable;
			case INDICATOR_BAR_CHART -> M.IndicatorBarChart;
			case NORMALISATION_BAR_CHART -> M.NormalisationBarChart;
			case NORMALISATION_RADAR_CHART -> M.NormalisationRadarChart;
			case RELATIVE_INDICATOR_BAR_CHART -> M.RelativeLciaResultsBarChart;
			case RELATIVE_INDICATOR_RADAR_CHART -> M.RelativeLciaResultsRadarChart;
			case SINGLE_SCORE_BAR_CHART -> M.SingleScoreBarChart;
			case LCC_ADDED_VALUES_TABLE -> M.LCCAddedValuesTable;
			case LCC_NET_COSTS_TABLE -> M.LCCNetcostsTable;
		};
	}
}
