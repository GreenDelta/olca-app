import { Chart, ChartConfiguration } from "chart.js";
import React, { useEffect, useRef, useState } from "react";

import { IndicatorCombo } from "./charts";
import { hasResults, totalResultOf } from "../util";
import { Report, ReportIndicator } from "../model";

export const IndicatorBarChart = ({ report }: { report: Report }) => {

  const indicators = report.indicators;
  if (!hasResults(report)) {
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

function configOf(
  report: Report, indicator: ReportIndicator): ChartConfiguration {

  const results = [];
  const labels = [];
  for (const variant of report.variants) {
    labels.push(variant.name);
    results.push(totalResultOf(report, indicator, variant));
  }
  const unit = indicator.impact.referenceUnit || "";

  return {

    type: "bar",
    data: {
      labels,
      datasets: [{
        data: results,
        label: indicator.impact.name,
        borderColor: "#7b0052",
        backgroundColor: "#7b0052",
        maxBarThickness: 50,
      }],
    },

    options: {
      responsive: false,
      scales: {
        y: { title: { display: true, text: unit } }
      },
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: (item) => `${item.formattedValue} ${unit}`,
          }
        }
      }
    },
  };
}
