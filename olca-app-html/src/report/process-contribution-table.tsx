import React, { useState } from "react";
import { Report, VariantResult } from "./model";
import { formatScientific, hasResults, variantResultOf } from "./util";
import { IndicatorCombo } from "./indicator-combo";

export const ProcessContributionTable = ({ report }: { report: Report }) => {
    if (!hasResults(report)) {
        return <></>;
    }

    // the selected indicator
    const indicators = report.indicators;
    const [indicator, setIndicator] = useState(indicators[0]);

    const header = [<th key="_empty" />];
    const results: VariantResult[] = [];
    for (const variant of report.variants) {
        header.push(<th key={variant.name}>{variant.name}</th>);
        results.push(variantResultOf(report, indicator, variant));
    }

    const rows: JSX.Element[] = [];
    for (const process of report.processes) {
        const cells = [<td key={`${process.refId}-cell`}>{process.name}</td>];
        for (let i = 0; i < results.length; i++) {
            const result = results[i];
            const value = result.contributions?.[process.refId] || 0;
            cells.push(<td key={`${process.refId}-${i}`}>
                {formatScientific(value)} {indicator.impact.referenceUnit}
            </td>);
        }
        rows.push(<tr key={`${process.refId}-row`}>{cells}</tr>)
    }

    return (
        <div>
            <div style={{ textAlign: "center" }}>
                <IndicatorCombo
                    indicators={indicators}
                    selected={indicator}
                    onChange={i => setIndicator(i)} />
            </div>
            <table>
                <thead>
                    <tr>
                        {header}
                    </tr>
                </thead>
                <tbody>
                    {rows}
                </tbody>
            </table>
        </div>
    )
};