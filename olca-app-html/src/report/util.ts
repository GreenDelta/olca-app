import { Report, ReportIndicator, ReportVariant, VariantResult } from "./model";

export function isEmpty<T>(xs: T[]): boolean {
  return !xs || xs.length == 0;
}

export function hasResults(report: Report): boolean {
  if (!report
    || isEmpty(report.variants)
    || isEmpty(report.indicators)
    || isEmpty(report.results)) {
    return false;
  }
  return true;
}

/**
 * Returns the variant result for the given indicator and variant from a report.
 */
export function variantResultOf(report: Report, indicator: ReportIndicator,
  variant: ReportVariant): VariantResult {
  if (!report?.results || !indicator?.impact || !variant) {
    return null;
  }
  for (const result of report.results) {
    if (result.indicatorId !== indicator.impact.refId
      || isEmpty(result.variantResults)) {
      continue;
    }
    for (const vr of result.variantResults) {
      if (vr.variant === variant.name) {
        return vr;
      }
    }
  }
  return null;
}

export function totalResultOf(report: Report, indicator: ReportIndicator,
  variant: ReportVariant): number {
  return variantResultOf(report, indicator, variant)?.totalAmount || 0;
}

export function normalizedResultOf(
  report: Report, indicator: ReportIndicator, variant: ReportVariant): number {
  const result = variantResultOf(report, indicator, variant);
  const total = result?.totalAmount || 0;
  const normValue = indicator?.normalisationFactor || 0;
  return normValue === 0
    ? 0
    : total / normValue;
};

export function singleScoreOf(
  report: Report, indicator: ReportIndicator, variant: ReportVariant): number {
  const n = normalizedResultOf(report, indicator, variant);
  const w = indicator?.weightingFactor || 0;
  return n * w;
};

export function formatScientific(n: number, digits=5): string {
  if (typeof n !== "number") {
    return "";
  }
  return n.toExponential(digits);
}
