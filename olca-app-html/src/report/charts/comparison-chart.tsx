import { ChartConfiguration, ChartData, ChartDataset } from "chart.js";
import { Report } from "../model";
import { formatScientific, normalizedResultOf, totalResultOf } from "../util";
import { colorOf, staticChartOf } from "./charts";

type CompProps = {
  report: Report,
  type: "bar" | "radar",
  normalized?: boolean,
};
/** A bar or radar chart that compares all indicator values of all */
export const ComparisonChart = (props: CompProps) => {
  const { report, type } = props;
  const indicators = report.indicators;
  const variants = report.variants;
  if (!variants || variants.length === 0 ||
    !indicators || indicators.length === 0) {
    return null;
  }

  const config: ChartConfiguration = {
    type,
    data: comparisonData(props),
    options: {
      responsive: false,
      plugins: {
        legend: { position: "bottom" },
        tooltip: {
          callbacks: {
            label: (item) => {
              const value = item.raw as number;
              const num = props.normalized
                ? formatScientific(value, 3)
                : value.toFixed(2) + "%";
              return `${item.dataset.label}: ${num}`;
            }
          }
        }
      },
    },
  };

  if (type === "bar") {
    config.options.scales = {
      y: {
        ticks: {
          callback: (value) => props.normalized
            ? value
            : `${value}%`
        }
      }
    }
  } else if (type === "radar") {
    config.options.scales = {
      r: {
        ticks: { display: false }
      }
    }
  }

  return staticChartOf(config);
};


function comparisonData(p: CompProps): ChartData {
  const report = p.report;
  const indicators = report.indicators;
  const maxvals = p.normalized
    ? null
    : maxIndicatorResults(p.report);
  const labels = indicators.map(i => i.impact.name);

  const datasets = report.variants.map((variant, index): ChartDataset => {
    return {
      label: variant.name,
      borderColor: colorOf(index),
      backgroundColor: colorOf(index, p.type === "radar" ? 0.2 : null),
      maxBarThickness: 50,
      data: indicators.map(indicator => {
        if (p.normalized) {
          return normalizedResultOf(p.report, indicator, variant);
        }
        const result = totalResultOf(p.report, indicator, variant);
        const max = maxvals[indicator.impact.refId];
        return !max
          ? 0
          : 100 * result / max;
      }),
    }
  });

  return { labels, datasets };
}

function maxIndicatorResults(report: Report): Record<string, number> {
  const maxVals: Record<string, number> = {};
  for (const indicator of report.indicators) {
    let max = 0;
    for (const variant of report.variants) {
      const next = Math.abs(totalResultOf(report, indicator, variant));
      max = Math.max(max, next);
    }
    maxVals[indicator.impact.refId] = max;
  }
  return maxVals;
}
