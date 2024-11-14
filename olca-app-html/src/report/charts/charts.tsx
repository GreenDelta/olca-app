import React, { useEffect, useRef } from "react";
import { Chart, ChartConfiguration } from "chart.js";

/**
 * Creates a static chart from the given configuration.
 *
 * @param config the chart configuration
 * @returns the wrapped chart canvas element
 */
export function staticChartOf(config: ChartConfiguration): JSX.Element {
  config.options.devicePixelRatio = 3 // increases resolution

  const canvas = useRef<HTMLCanvasElement>(null);
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

export function colorOf(i: number, alpha?: number): string {
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
  let color;
  if (i < colors.length) {
    color = colors[i];
  } else {
    const gen = () => Math.round(Math.random() * 255);
    color = `${gen()}, ${gen()}, ${gen()}`;
  }
  return alpha
    ? `rgba(${color}, ${alpha})`
    : "rgb(" + color + ")"
}
