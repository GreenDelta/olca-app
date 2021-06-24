import React from "react";
import {
  Report,
  ReportIndicator,
  ReportVariant,
  ReportParameter,
  ReportCostResult,
} from "./model";
import { formatScientific, normalizedResultOf, singleScoreOf, totalResultOf } from "./util";

type Props = { report: Report };

export const VariantDescriptionTable = ({ report }: Props) => {
  const row = (v: ReportVariant) => {
    return (
      <tr key={v.name}>
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
  const row = (i: ReportIndicator) => {
    return (
      <tr key={i.impact.refId}>
        <td>{i.impact.name}</td>
        <td>{i.impact.referenceUnit}</td>
        <td>{i.impact.description}</td>
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
  const row = (p: ReportParameter) => {
    if (!p) {
      return null;
    }
    return (
      <tr key={_key(p.redef.name)}>
        <td>{p.redef.name}</td>
        <td>{p.context?.name || "global"}</td>
        <td>{p.redef.description}</td>
      </tr>
    );
  };
  return (
    <div>
      <table>
        {_head("Parameter", "Context", "Description")}
        {_rows(report.parameters, row)}
      </table>
    </div>
  );
};

export const ParameterValueTable = ({ report }: Props) => {
  const header = ["Parameter", "Context"];
  report.variants?.forEach(v => header.push(v.name));

  const row = (p: ReportParameter) => (
    <tr>
      <td>{p.redef.name}</td>
      <td>{p.context?.name || "global"}</td>
      {report.variants?.map(v => (
        <td key={_key(v.name)}>
          {p.variantValues?.[v.name] || 0}
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
  const header = ["Indicator"];
  report.variants?.forEach(v => header.push(v.name));

  if (!normalized && !singleScore) {
    header.push("Unit");
  }

  const amount = (v: ReportVariant, i: ReportIndicator) => {
    if (normalized) {
      return formatScientific(normalizedResultOf(report, i, v));
    }
    if (singleScore) {
      return formatScientific(singleScoreOf(report, i, v));
    }
    return formatScientific(totalResultOf(report, i, v));
  };

  const row = (i: ReportIndicator) => {
    let unit = null;
    if (!normalized && !singleScore) {
      const u = i.impact.referenceUnit || "";
      unit = <td>{u}</td>;
    }
    return (
      <tr key={i.impact.refId}>
        <td>{i.impact.name}</td>
        {report.variants?.map(v => (
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
  const isPresent = report.variants.reduce(
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
