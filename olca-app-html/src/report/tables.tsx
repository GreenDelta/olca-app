import React from "react";
import {
    Report, ReportIndicator, ReportVariant, ReportParameter,
    getVariantResult, getNormalizedResult, getSingleScore, ReportCostResult,
    getVariants, scientific,
} from "./model";

type Props = { report: Report };

export const VariantDescriptionTable = ({ report }: Props) => {
    const row = (v: ReportVariant) => {
        if (v.isDisabled) {
            return null;
        }
        return (
            <tr key={v.id}>
                <td>{v.name}</td>
                <td>{v.description}</td>
            </tr>
        );
    };
    return (
        <div>
            <table>
                {_head("Variant", "Description")}
                {_rows(report.variants, row)}
            </table>
        </div>
    );
};

export const IndicatorDescriptionTable = ({ report }: Props) => {
    const row = (indicator: ReportIndicator) => {
        if (!indicator || !indicator.displayed) {
            return null;
        }
        const unit = indicator.descriptor
            ? indicator.descriptor.referenceUnit : "";
        return (
            <tr key={indicator.id}>
                <td>{indicator.reportName}</td>
                <td>{unit}</td>
                <td>{indicator.reportDescription}</td>
            </tr>
        );
    };
    return (
        <div>
            <table>
                {_head("Indicator", "Unit", "Description")}
                {_rows(report.indicators, row)}
            </table>
        </div>
    );
};

export const ParameterDescriptionTable = ({ report }: Props) => {
    const row = (param: ReportParameter) => {
        if (!param) {
            return null;
        }
        return (
            <tr key={_key(param.name)}>
                <td>{param.name}</td>
                <td>{param.description}</td>
            </tr>
        );
    };
    return (
        <div>
            <table>
                {_head("Parameter", "Description")}
                {_rows(report.parameters, row)}
            </table>
        </div>
    );
};

export const ParameterValueTable = ({ report }: Props) => {
    const header = ["Parameter"];
    const variants = getVariants(report);
    variants.forEach((v) => header.push(v.name));

    const row = (param: ReportParameter) => (
        <tr>
            <td>{param.name}</td>
            {variants.map((v) => (
                <td key={_key(v.name)}>
                    {param.variantValues[v.id]}
                </td>
            ))}
        </tr>
    );
    return (
        <div>
            <table>
                {_head(...header)}
                {_rows(report.parameters, row)}
            </table>
        </div>
    );
};

type ResultProps = Props & {
    normalized?: boolean;
    singleScore?: boolean;
};
export const ResultTable = ({ report, normalized, singleScore }: ResultProps) => {
    const variants = getVariants(report);
    const header = ["Indicator"];
    variants.forEach((v) => header.push(v.name));

    if (!normalized && !singleScore) {
        header.push("Unit");
    }

    const amount = (v: ReportVariant, i: ReportIndicator) => {
        if (normalized) {
            return scientific(getNormalizedResult(report, v, i));
        }
        if (singleScore) {
            return scientific(getSingleScore(report, v, i));
        }
        return scientific(getVariantResult(report, v, i));
    };

    const row = (i: ReportIndicator) => {
        if (!i.displayed) {
            return null;
        }
        let unit = null;
        if (!normalized && !singleScore) {
            const u = i.descriptor ? i.descriptor.referenceUnit : "";
            unit = <td>{u}</td>;
        }
        return (
            <tr key={i.id}>
                <td>{i.reportName}</td>
                {variants.map((v) => (
                    <td key={_key(v.name)}>
                        {amount(v, i)}
                    </td>
                ))}
                {unit}
            </tr>
        );
    };

    return (
        <div>
            <table>
                {_head(...header)}
                {_rows(report.indicators, row)}
            </table>
        </div>
    );
};

type CostProps = Props & { addedValue?: boolean };
export const CostResultTable = ({ report, addedValue }: CostProps) => {
    const isPresent = getVariants(report).reduce(
        (m: Record<string, boolean>, v) => {
            m[v.name] = true;
            return m;
        }, {});

    const row = (cr: ReportCostResult) => {
        if (!isPresent[cr.variant]) {
            return null;
        }
        return (
            <tr key={_key(cr.variant)}>
                <td>{cr.variant}</td>
                <td>{cr.value}</td>
            </tr>
        );
    };

    return (
        <div>
            <table>
                {_head("Variant", (addedValue ? "Added value" : "Net-costs"))}
                {_rows((addedValue ? report.addedValues : report.netCosts), row)}
            </table>
        </div>
    );
};

function _rows<T>(models: T[], fn: (m: T) => JSX.Element | null): JSX.Element {
    if (!models) {
        return <tbody />;
    }
    const rows = [];
    for (const model of models) {
        const elem = fn(model);
        if (elem) {
            rows.push(elem);
        }
    }
    return <tbody>{rows}</tbody>;
}

function _head(...cols: string[]) {
    return (
        <thead>
            <tr>
                {cols.map((col) => (
                    <th key={_key(col)}>{col}</th>
                ))}
            </tr>
        </thead>
    );
}

function _key(s: string): string {
    if (!s) {
        return "none";
    }
    return s.toLowerCase().replace(" ", "-");
}
