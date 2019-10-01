import React, { useEffect, useRef, useState } from "react";
import {
    Report, ReportVariant, ReportIndicator, getVariantResult,
    getNormalizedResult, getIndicators, getVariants, scientific, getSingleScore, getContribution,
} from "./model";
import { Chart, ChartData, ChartConfiguration } from "chart.js";

type IndicatorConfig = { report: Report; contributions?: boolean };
export const IndicatorChart = ({ report, contributions }: IndicatorConfig) => {
    const indicators = getIndicators(report);
    const variants = getVariants(report);
    if (!variants || variants.length === 0 ||
        !indicators || indicators.length === 0) {
        return null;
    }

    // we store the index of the selected indicator as state
    const [indicatorIdx, setIndicatorIdx] = useState(0);

    const canvas = useRef(null);
    useEffect(() => {
        const indicator = indicators[indicatorIdx];
        let data: ChartData;
        if (contributions !== true || !report.processes) {
            data = {
                labels: variants.map((v) => v.name),
                datasets: [{
                    label: indicator.reportName,
                    borderColor: "#7b0052",
                    backgroundColor: "#7b0052",
                    data: variants.map((v) => getVariantResult(report, v, indicator)),
                }],
            };
        } else {
            data = {
                labels: variants.map((v) => v.name),
                datasets: report.processes.map((process, idx) => {
                    return {
                        label: process.reportName,
                        backgroundColor: _color(idx),
                        data: variants.map((variant) => getContribution(
                            { report, indicator, variant, process })),
                    };
                }),
            };
            data.datasets.push({
                label: "Other",
                backgroundColor: "rgba(121, 121, 121, 0.5)",
                data: variants.map((variant) => getContribution(
                    { report, indicator, variant, rest: true })),
            });
        }

        const config = _barConfig(data, {
            legend: contributions === true,
            stacked: contributions === true,
        });
        const ctxt = canvas.current.getContext("2d");
        const chart = new Chart(ctxt, config);
        return () => {
            if (chart) {
                chart.destroy();
            }
        };
    });

    return (
        <div style={{ textAlign: "center" }}>
            <form>
                <fieldset>
                    <select value={indicatorIdx} style={{ width: 300 }}
                        onChange={(e) => setIndicatorIdx(parseInt(e.target.value, 10))}>
                        {indicators.map((indicator, idx) => (
                            <option key={indicator.id} value={idx}>
                                {indicator.reportName}
                            </option>
                        ))}
                    </select>
                </fieldset>
            </form>
            <canvas width="650" height="400" ref={canvas}
                style={{ display: "inline-block" }} />
        </div>
    );
};

export const SingleScoreChart = ({ report }: { report: Report }) => {
    const indicators = getIndicators(report);
    const variants = getVariants(report);
    if (!variants || variants.length === 0 ||
        !indicators || indicators.length === 0) {
        return null;
    }
    const data: ChartData = {
        labels: variants.map((v) => v.name),
        datasets: indicators.map((i, idx) => {
            return {
                label: i.reportName,
                backgroundColor: _color(idx),
                data: variants.map((v) => getSingleScore(report, v, i)),
            };
        }),
    };
    const config = _barConfig(data, { stacked: true });
    return _staticChart(config);
};

type CompProps = {
    report: Report,
    type: "bar" | "radar",
    normalized?: boolean,
};
/** A bar or radar chart that compares all indicator values of all */
export const ComparisonChart = (props: CompProps) => {
    const { report, type } = props;
    const indicators = getIndicators(report);
    const variants = getVariants(report);
    if (!variants || variants.length === 0 ||
        !indicators || indicators.length === 0) {
        return null;
    }

    const config: ChartConfiguration = {
        type,
        data: _comparisonData(props, variants, indicators),
        options: {
            responsive: false,
            legend: { position: "bottom" },
        },
    };

    if (type === "bar") {
        config.options.scales = {
            xAxes: [{ maxBarThickness: 50 }],
            yAxes: [{
                ticks: {
                    beginAtZero: true,
                    callback: (value) => props.normalized
                        ? scientific(value) : `${value}%`,
                },
            }],
        };
        config.options.tooltips = {
            callbacks: {
                label: (item) => {
                    const value = parseFloat(item.value);
                    const s = props.normalized
                        ? scientific(value)
                        : `${Math.round(value)}%`;
                    return `${item.label}: ${s}`;
                },
            },
        };
    }

    if (type === "radar") {
        config.options.scale = {
            ticks: {
                beginAtZero: true,
                display: false,
            },
        };
    }

    return _staticChart(config);
};

function _staticChart(config: ChartConfiguration) {
    const canvas = useRef(null);
    let chart: Chart | null = null;
    useEffect(() => {
        if (chart) {
            return;
        }
        const ctxt = canvas.current.getContext("2d");
        chart = new Chart(ctxt, config);
        return () => {
            if (chart) {
                chart.destroy();
                chart = null;
            }
        };
    });

    return (
        <div style={{ textAlign: "center" }}>
            <canvas width="650" height="400" ref={canvas}
                style={{ display: "inline-block" }} />
        </div>
    );
}

function _barConfig(data: ChartData,
    conf?: { legend?: boolean, stacked?: boolean }): ChartConfiguration {
    const hideLegend = conf && conf.legend === false;
    const stacked = conf && conf.stacked === true;
    return {
        type: "bar", data,
        options: {
            responsive: false,
            legend: { display: !hideLegend, position: "bottom" },
            scales: {
                xAxes: [{
                    stacked,
                    maxBarThickness: 50,
                }],
                yAxes: [{
                    stacked,
                    ticks: { beginAtZero: true },
                }],
            },
            tooltips: {
                callbacks: {
                    label: (item) => {
                        const value = parseFloat(item.value);
                        return `${item.label}: ${scientific(value)}`;
                    },
                },
            },
        },
    };
}

function _color(i: number, alpha?: number): string {
    const colors = [
        "229, 48, 57",
        "41, 111, 196",
        "255, 201, 35",
        "82, 168, 77",
        "132, 76, 173",
        "127, 183, 229",
        "255, 137, 0",
        "128, 0, 128",
        "135, 76, 63",
        "252, 255, 100",
        "0, 177, 241",
        "112, 187, 40",
        "18, 89, 133",
        "226, 0, 115",
        "255, 255, 85",
        "218, 0, 24",
        "0, 111, 154",
        "255, 153, 0",
    ];
    if (i >= colors.length) {
        return "rgb(0, 0, 0)";
    }
    if (!alpha) {
        return "rgb(" + colors[i] + ")";
    }
    return `rgba(${colors[i]}, ${alpha})`;
}

/** Returns the maximum indicator values in a map: indicator ID -> max. */
function _maxIndicatorValues(report: Report, variants: ReportVariant[],
    indicators: ReportIndicator[]): Record<number, number> {
    type NMap = Record<number, number>;
    const maxvals: NMap = indicators.reduce((m: NMap, indicator) => {
        let max: number = variants.reduce((m: number, variant) => {
            const result = Math.abs(
                getVariantResult(report, variant, indicator));
            return Math.max(result, m);
        }, 0);
        max = max === 0 ? 1 : max;
        m[indicator.id] = max;
        return m;
    }, {});
    return maxvals;
}

function _comparisonData(p: CompProps, variants: ReportVariant[],
    indicators: ReportIndicator[]): ChartData {
    const maxvals = p.normalized ? null : _maxIndicatorValues(
        p.report, variants, indicators);
    return {
        labels: indicators.map((i) => i.reportName),
        datasets: variants.map((v, index) => {
            return {
                label: v.name,
                borderColor: _color(index),
                backgroundColor: _color(index, p.type === "radar" ? 0.2 : null),
                data: indicators.map((i) => {
                    if (p.normalized) {
                        return getNormalizedResult(p.report, v, i);
                    }
                    const result = getVariantResult(p.report, v, i);
                    return 100 * result / maxvals[i.id];
                }),
            };
        }),
    };
}
