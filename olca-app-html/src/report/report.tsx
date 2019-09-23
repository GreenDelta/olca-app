import React from "react";
import { render } from "react-dom";

import { Report, ReportSection } from "./model";
import {
    VariantDescriptionTable, IndicatorDescriptionTable,
    ParameterDescriptionTable, ParameterValueTable,
    ResultTable,
    CostResultTable,
} from "./tables";
import { ComparisonChart, IndicatorChart, SingleScoreChart } from "./charts";

type Props = { report: Report };

const Page = ({ report }: Props) => {
    const sections: JSX.Element[] = [];
    if (report.sections) {
        report.sections.forEach((s) => {
            sections.push(
                <Section key={s.index} section={s} report={report} />
            );
        });
    }
    return (
        <div className="container" style={{ marginTop: 25 }}>
            <h1>{report.title}</h1>
            {sections}
        </div>
    );
};

type SectionProps = { section: ReportSection; report: Report; };
const Section = ({ section, report }: SectionProps) => {
    const component = getSectionComponent(section.componentId, report);
    return (
        <div>
            <h3>{section.title}</h3>
            <p dangerouslySetInnerHTML={{ __html: section.text }} />
            {component}
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
            return <IndicatorChart report={report} />;
        case "single_score_bar_chart":
            return <SingleScoreChart report={report} />;
        case "process_contribution_chart":
            return <IndicatorChart report={report} contributions />;
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
