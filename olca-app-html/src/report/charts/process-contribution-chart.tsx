import { Chart, ChartConfiguration } from "chart.js";
import React, { useEffect, useRef, useState } from "react";

import {
  ProcessDescriptor,
  Report,
  ReportIndicator,
  ReportVariant,
} from "../model";
import { colorOf, IndicatorCombo } from "./charts";
import { hasResults, isEmpty, variantResultOf } from "../util";

export const ProcessContributionChart = ({ report }: { report: Report }) => {
  const indicators = report.indicators;
  if (!hasResults(report)
    || isEmpty(report.results)) {
    return <></>;
  }

  // the index of the selected indicator
  const [indicatorIdx, setIndicatorIdx] = useState(0);

  const canvas = useRef<HTMLCanvasElement>(null);
  useEffect(() => {
    const indicator = indicators[indicatorIdx];
    const config = configOf(report, indicator);
    const context2d = canvas.current?.getContext("2d");
    const chart = context2d
      ? new Chart(context2d, config)
      : null;
    return () => chart?.destroy();
  });

  return (
    <div style={{ textAlign: "center" }}>
      <IndicatorCombo
        indicators={indicators}
        selectedIndex={indicatorIdx}
        onChange={setIndicatorIdx} />
      <canvas width="650" height="400" ref={canvas}
        style={{ display: "inline-block" }} />
    </div>
  );

}

function configOf(report: Report, indicator: ReportIndicator):
  ChartConfiguration {

  const variants = report.variants;

  const datasets = [];
  datasets.push({
    label: "Other",
    backgroundColor: "rgba(121, 121, 121, 0.5)",
    maxBarThickness: 50,
    data: variants.map(v => restOf(report, indicator, v)),
  });
  for (let i = 0; i < report.processes.length; i++) {
    const process = report.processes[i];
    datasets.push({
      label: process.name,
      backgroundColor: colorOf(i),
      maxBarThickness: 50,
      data: variants.map(v => contributionOf(report, indicator, v, process))
    })
  }

  const unit = indicator.impact.referenceUnit || "";

  return {
    type: "bar",
    data: {
      labels: variants.map(variant => variant.name),
      datasets,
    },
    options: {
      responsive: false,
      scales: {
        x: { stacked: true },
        y: {
          stacked: true,
          title: { display: true, text: unit }
        }
      },
      plugins: {
        legend: { display: true, position: "bottom" },
        tooltip: {
          callbacks: {
            label: (item) => {
              const num = (item.raw as number).toExponential(3);
              return `${num} ${unit} : ${item.dataset.label}`;
            },
          }
        }
      }
    },
  }
}

function contributionOf(report: Report, indicator: ReportIndicator,
  variant: ReportVariant, process: ProcessDescriptor): number {
  const result = variantResultOf(report, indicator, variant);
  if (!result || !result.contributions) {
    return 0;
  }
  return result.contributions[process.refId] || 0;
};

function restOf(report: Report, indicator: ReportIndicator,
  variant: ReportVariant): number {
  const result = variantResultOf(report, indicator, variant);
  if (!result) {
    return 0;
  }
  let rest = result.totalAmount;
  if (!result.contributions) {
    return rest;
  }
  for (const processId of Object.keys(result.contributions)) {
    const c = result.contributions[processId] || 0;
    rest -= c;
  }
  return rest;
}
