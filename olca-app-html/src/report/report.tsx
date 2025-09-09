import React from "react";
import { render } from "react-dom";

import { Report } from "./model";
import {
  VariantDescriptionTable,
  IndicatorDescriptionTable,
  ParameterDescriptionTable,
  ParameterValueTable,
  ResultTable,
  CostResultTable,
} from "./tables";

import { IndicatorBarChart } from "./charts/indicator-bar-chart";
import { ProcessContributionChart } from "./charts/process-contribution-chart";
import { SingleScoreChart } from "./charts/single-score-bar-chart";
import { ComparisonChart } from "./charts/comparison-chart";
import { ProcessContributionTable } from "./process-contribution-table";

const Page = ({ report }: { report: Report }) => {
  const sections: JSX.Element[] = [];
  if (report.sections) {
    for (const section of report.sections) {
      const component = getSectionComponent(section.componentId, report);
      sections.push(
        <div key={section.index}>
          <h3>{section.title}</h3>
          <p dangerouslySetInnerHTML={{ __html: section.text }} />
          {component}
        </div>
      );
    }
  }
  return (
    <div className="container" style={{ marginTop: 25 }}>
      <h1>{report.title}</h1>
      {sections}
    </div>
  );
};

const getSectionComponent = (id: string, report: Report) => {
  if (!id) {
    return null;
  }
  switch (id) {
    case "variant_description_table":
      return <VariantDescriptionTable report={report} />;
    case "indicator_description_table":
      return <IndicatorDescriptionTable report={report} />;
    case "parameter_description_table":
      return <ParameterDescriptionTable report={report} />;
    case "parameter_value_table":
      return <ParameterValueTable report={report} />;
    case "impact_result_table":
      return <ResultTable report={report} />;
    case "normalisation_result_table":
      return <ResultTable report={report} normalized />;
    case "single_score_table":
      return <ResultTable report={report} singleScore />;
    case "lcc_net_costs_table":
      return <CostResultTable report={report} />;
    case "lcc_added_values_table":
      return <CostResultTable report={report} addedValue />;
    case "relative_indicator_bar_chart":
      return <ComparisonChart report={report} type="bar" />;
    case "relative_indicator_radar_chart":
      return <ComparisonChart report={report} type="radar" />;
    case "normalisation_bar_chart":
      return <ComparisonChart report={report} type="bar" normalized />;
    case "normalisation_radar_chart":
      return <ComparisonChart report={report} type="radar" normalized />;
    case "indicator_bar_chart":
      return <IndicatorBarChart report={report} />;
    case "single_score_bar_chart":
      return <SingleScoreChart report={report} />;
    case "process_contribution_chart":
      return <ProcessContributionChart report={report} />;
    case "process_contribution_table":
      return <ProcessContributionTable report={report} />;
    default:
      return null;
  }
};

const setData = (report: Report) => {
  render(<Page report={report} />,
    document.getElementById("react-root"));
};

declare global {
  interface Window {
    setData: any;
  }
}
window.setData = setData;

// Hot Module Replacement (HMR) support
if (module.hot) {
  module.hot.accept();
}
