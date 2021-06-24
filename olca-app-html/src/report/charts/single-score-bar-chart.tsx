import React from "react";
import { ChartConfiguration, ChartData } from "chart.js";
import { Report } from "../model";
import { colorOf, staticChartOf } from "./charts";
import { hasResults, singleScoreOf } from "../util";

export const SingleScoreChart = ({ report }: { report: Report }) => {

  if (!hasResults(report)) {
    return <></>;
  }

  // create the chart data
  const indicators = report.indicators;
  const variants = report.variants;
  const data: ChartData = {
    labels: variants.map((v) => v.name),
    datasets: indicators.map((i, idx) => {
      return {
        label: i.impact.name,
        backgroundColor: colorOf(idx),
        maxBarThickness: 50,
        data: variants.map((v) => singleScoreOf(report, i, v)),
      };
    }),
  };

  // create the chart configuration
  const config: ChartConfiguration = {
    type: "bar",
    data,
    options: {
      responsive: false,
      scales: {
        x: {stacked: true},
        y: {stacked: true},
      },
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: (item) => {
              const num = (item.raw as number).toExponential(3);
              return `${num} : ${item.dataset.label}`;
            },
          },
        },
      }
    },
  };
  return staticChartOf(config);
};
