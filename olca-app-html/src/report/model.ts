export type Report = {
    title: string;
    withNormalisation: boolean;
    withWeighting: boolean;
    project?: BaseDescriptor;
    sections?: ReportSection[];
    parameters?: ReportParameter[];
    variants?: ReportVariant[];
    indicators?: ReportIndicator[];
    processes?: ReportProcess[];
    results?: ReportIndicatorResult[];
    addedValues?: ReportCostResult[];
    netCosts?: ReportCostResult[];
};

export type BaseDescriptor = {
    refId: string;
    name: string;
    description: string;
};

export type IndicatorDescriptor = BaseDescriptor & {
    referenceUnit: string;
};

export type ReportIndicator = {
    id: number;
    descriptor: IndicatorDescriptor;
    reportName: string;
    reportDescription: string;
    displayed: boolean;
    normalisationFactor?: number;
    weightingFactor?: number;
};

export type ReportSection = {
    index: number;
    title: string;
    text: string;
    componentId?: string;
};

export type ReportParameter = {
    name: string;
    description: string;
    variantValues: Record<number, number>;
};

export type ReportVariant = {
    id: number;
    name: string;
    description: string;
    isDisabled: boolean;
};

export type ReportProcess = {
    id: number;
    descriptor: BaseDescriptor;
    reportName: string;
    reportDescription: string;
};

export type ReportIndicatorResult = {
    indicatorId: number;
    variantResults?: VariantResult[];
};

export type VariantResult = {
    variant: string;
    totalAmount: number;
    contributions?: Contribution[];
};

export type Contribution = {
    processId: number;
    amount: number;
    rest: boolean;
};

export type ReportCostResult = {
    variant: string;
    value: string; // Value + currency.
};

export const getVariantResult = (r: Report, v: ReportVariant,
    i: ReportIndicator): number => {
    if (!r.results) {
        return 0;
    }
    for (const result of r.results) {
        if (result.indicatorId !== i.id || !result.variantResults) {
            continue;
        }
        for (const vr of result.variantResults) {
            if (vr.variant === v.name) {
                return vr.totalAmount ? vr.totalAmount : 0;
            }
        }
    }
    return 0;
};

export const getNormalizedResult = (r: Report, v: ReportVariant,
    i: ReportIndicator): number => {
    const result = getVariantResult(r, v, i);
    if (result === 0 || !i.normalisationFactor) {
        return 0;
    }
    return result / i.normalisationFactor;
};

export const getSingleScore = (r: Report, v: ReportVariant,
    i: ReportIndicator): number => {
    const n = getNormalizedResult(r, v, i);
    if (n === 0 || !i.weightingFactor) {
        return 0;
    }
    return n * i.weightingFactor;
};

export const getVariants = (r: Report): ReportVariant[] => {
    if (!r.variants) {
        return [];
    }
    return r.variants.filter((v) => !v.isDisabled);
};

type ContributionOptions = {
    report: Report;
    variant: ReportVariant;
    indicator: ReportIndicator;
    process?: ReportProcess;
    rest?: boolean;
};
export const getContribution = (c: ContributionOptions): number => {
    if (!c.report.results) {
        return 0;
    }
    for (const result of c.report.results) {
        if (result.indicatorId !== c.indicator.id
            || !result.variantResults) {
            continue;
        }
        for (const vr of result.variantResults) {
            if (vr.variant !== c.variant.name || !vr.contributions) {
                continue;
            }
            for (const con of vr.contributions) {
                if (c.rest && con.rest) {
                    return con.amount;
                }
                if (c.process && c.process.id === con.processId) {
                    return con.amount;
                }
            }
        }
    }
    return 0;
};

export const scientific = (n: number): string => {
    if (!n) {
        return "0";
    }
    return n.toExponential(5);
};

export const getIndicators = (r: Report): ReportIndicator[] => {
    if (!r.indicators) {
        return [];
    }
    return r.indicators.filter((i) => i.displayed);
};
