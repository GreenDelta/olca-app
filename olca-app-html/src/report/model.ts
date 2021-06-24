export type Report = {
  title: string;
  withNormalisation: boolean;
  withWeighting: boolean;
  sections?: ReportSection[];
  processes?: ProcessDescriptor[];

  variants?: ReportVariant[];
  parameters?: ReportParameter[];
  indicators?: ReportIndicator[];
  results?: ReportImpactResult[];
  addedValues?: ReportCostResult[];
  netCosts?: ReportCostResult[];
};

export type Descriptor = {
  refId: string;
  name: string;
  description?: string;
};

export type ImpactDescriptor = Descriptor & {
  referenceUnit?: string;
};

export type ProcessDescriptor = Descriptor;

export type ParameterRedef = {
  name: string;
  description?: string;
  value: number;
};

export type ReportParameter = {
  redef: ParameterRedef;
  context?: Descriptor;
  variantValues: Record<string, number>;
};

export type ReportIndicator = {
  impact: ImpactDescriptor;
  normalisationFactor?: number;
  weightingFactor?: number;
};

export type ReportSection = {
  index: number;
  title: string;
  text: string;
  componentId?: string;
};

export type ReportVariant = {
  name: string;
  description?: string;
};

export type ReportImpactResult = {
  indicatorId: string;
  variantResults?: VariantResult[];
};

export type VariantResult = {
  variant: string;
  totalAmount: number;
  contributions?: Record<string, number>;
};

export type ReportCostResult = {
  variant: string;
  currency: string;
  value: number;
};
