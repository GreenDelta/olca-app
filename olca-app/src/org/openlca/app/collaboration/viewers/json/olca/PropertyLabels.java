package org.openlca.app.collaboration.viewers.json.olca;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.openlca.app.M;
import org.openlca.app.collaboration.viewers.json.content.JsonNode;
import org.openlca.core.model.ModelType;

class PropertyLabels {

	private static final EnumMap<ModelType, Map<String, String>> labels = new EnumMap<>(ModelType.class);
	private static final EnumMap<ModelType, Map<String, ModelType>> imageTypes = new EnumMap<>(ModelType.class);
	private static final EnumMap<ModelType, Map<String, Integer>> ordinals = new EnumMap<>(ModelType.class);

	static {
		putLabels();
	}

	static String get(JsonNode node) {
		return get(labels, node, PropertyLabels::propertyOf);
	}

	private static <T> T get(EnumMap<ModelType, Map<String, T>> map, JsonNode node, Function<String, T> defaultValue) {
		var type = ModelUtil.typeOf(node);
		var path = ModelUtil.pathOf(node);
		if (!map.containsKey(type))
			return defaultValue != null ? defaultValue.apply(path) : null;
		var subMap = map.get(type);
		if (subMap.containsKey(path))
			return subMap.get(path);
		return defaultValue != null ? defaultValue.apply(path) : null;
	}

	private static String propertyOf(String path) {
		if (path.contains("."))
			return path.substring(path.lastIndexOf(".") + 1);
		return path;
	}

	static int getOrdinal(JsonNode node) {
		return get(ordinals, node, path -> 0);
	}

	static ModelType getImageType(JsonNode node) {
		return get(imageTypes, node, null);
	}

	private static void putLabels() {
		putLocationLabels();
		putCurrencyLabels();
		putActorLabels();
		putSourceLabels();
		putUnitGroupLabels();
		putFlowPropertyLabels();
		putSocialIndicatorLabels();
		putFlowLabels();
		putProcessLabels();
		putParameterLabels();
		putImpactMethodLabels();
		putImpactCategoryLabels();
		putProductSystemLabels();
		putProjectLabels();
		putDQSystemLabels();
		putResultLabels();
		putEpdLabels();
	}

	private static void putBasicLabels(ModelType type) {
		putBasicLabels(type, null);
	}

	private static void putBasicLabels(ModelType type, String path) {
		var prefix = path != null ? path + "." : "";
		put(type, prefix + "name", M.Name);
		put(type, prefix + "description", M.Description);
		if (path == null) {
			put(type, "category", M.Category);
			put(type, "tags", "Tags");
		}
	}

	private static void putLocationLabels() {
		var type = ModelType.LOCATION;
		putBasicLabels(type);
		put(type, "code", M.Code);
		put(type, "longitude", M.Longitude);
		put(type, "latitude", M.Latitude);
		put(type, "geometry", M.Geometry);
	}

	private static void putCurrencyLabels() {
		var type = ModelType.CURRENCY;
		putBasicLabels(type);
		put(type, "code", M.Code);
		put(type, "conversionFactor", M.ConversionFactor);
		put(type, "refCurrency", M.ReferenceCurrency, ModelType.CURRENCY);
	}

	private static void putActorLabels() {
		var type = ModelType.ACTOR;
		putBasicLabels(type);
		put(type, "address", M.Address);
		put(type, "city", M.City);
		put(type, "country", M.Country);
		put(type, "email", M.Email);
		put(type, "telefax", M.Telefax);
		put(type, "telephone", M.Telephone);
		put(type, "website", M.Website);
		put(type, "zipCode", M.ZipCode);
	}

	private static void putSourceLabels() {
		var type = ModelType.SOURCE;
		putBasicLabels(type);
		put(type, "url", M.URL);
		put(type, "textReference", M.TextReference);
		put(type, "year", M.Year);
		put(type, "externalFile", M.ExternalFile);
	}

	private static void putUnitGroupLabels() {
		var type = ModelType.UNIT_GROUP;
		putBasicLabels(type);
		put(type, "defaultFlowProperty", M.DefaultFlowProperty, ModelType.FLOW_PROPERTY);
		put(type, "units", M.Units, ModelType.UNIT);
		putUnitLabels(type, "units");
	}

	private static void putUnitLabels(ModelType type, String path) {
		putBasicLabels(type, path);
		put(type, path + ".conversionFactor", M.ConversionFactor);
		put(type, path + ".isRefUnit", M.ReferenceUnit);
		put(type, path + ".synonyms", M.Synonyms);
	}

	private static void putFlowPropertyLabels() {
		var type = ModelType.FLOW_PROPERTY;
		putBasicLabels(type);
		put(type, "unitGroup", M.UnitGroup, ModelType.UNIT_GROUP);
		put(type, "flowPropertyType", M.FlowPropertyType);
	}

	private static void putSocialIndicatorLabels() {
		var type = ModelType.SOCIAL_INDICATOR;
		putBasicLabels(type);
		put(type, "unitOfMeasurement", M.UnitOfMeasurement);
		put(type, "evaluationScheme", M.EvaluationSchema);
		put(type, "activityVariable", M.ActivityVariable);
		put(type, "activityQuantity", M.ActivityQuantity, ModelType.FLOW_PROPERTY);
		put(type, "activityUnit", M.ActivityUnit, ModelType.UNIT);
	}

	private static void putFlowLabels() {
		var type = ModelType.FLOW;
		putBasicLabels(type);
		put(type, "flowType", M.FlowType);
		put(type, "location", M.Location, ModelType.LOCATION);
		put(type, "isInfrastructureFlow", M.InfrastructureFlow);
		put(type, "cas", M.CASNumber);
		put(type, "formula", M.Formula);
		put(type, "synonyms", M.Synonyms);
		put(type, "flowProperties", M.FlowProperties, ModelType.FLOW_PROPERTY);
		putFlowPropertyFactorLabels(type, "flowProperties");
	}

	private static void putFlowPropertyFactorLabels(ModelType type, String path) {
		put(type, path + ".conversionFactor", M.ConversionFactor);
		put(type, path + ".isRefFlowProperty", M.ReferenceFlowProperty);
	}

	private static void putProcessLabels() {
		var type = ModelType.PROCESS;
		putBasicLabels(type);
		put(type, "processType", M.ProcessType);
		put(type, "location", M.Location, ModelType.LOCATION);
		put(type, "dqSystem", M.ProcessDataQualitySchema, ModelType.DQ_SYSTEM);
		put(type, "dqEntry", M.DataQualityEntry);
		put(type, "exchangeDqSystem", M.ExchangeDataQualitySchema, ModelType.DQ_SYSTEM);
		put(type, "socialDqSystem", M.SocialDataQualitySchema, ModelType.DQ_SYSTEM);
		put(type, "isInfrastructureProcess", M.InfrastructureProcess);
		put(type, "defaultAllocationMethod", M.AllocationMethod);
		put(type, "processDocumentation", M.ProcessDocumentation);
		putProcessDocumentationLabels(type, "processDocumentation");
		put(type, "inputs", M.Inputs, ModelType.FLOW);
		putExchangeLabels(type, "inputs");
		put(type, "outputs", M.Outputs, ModelType.FLOW);
		putExchangeLabels(type, "outputs");
		put(type, "allocationFactors", M.AllocationFactors);
		putAllocationFactorLabels(type, "allocationFactors");
		put(type, "socialAspects", M.SocialAspects, ModelType.SOCIAL_INDICATOR);
		putSocialAspectLabels(type, "socialAspects");
		put(type, "parameters", M.Parameters, ModelType.PARAMETER);
		putParameterLabels(type, "parameters");
	}

	private static void putProcessDocumentationLabels(ModelType type, String path) {
		put(type, path + ".creationDate", M.CreationDate);
		put(type, path + ".validFrom", M.StartDate);
		put(type, path + ".validUntil", M.EndDate);
		put(type, path + ".timeDescription", M.TimeDescription);
		put(type, path + ".geographyDescription", M.GeographyDescription);
		put(type, path + ".technologyDescription", M.TechnologyDescription);
		put(type, path + ".intendedApplication", M.IntendedApplication);
		put(type, path + ".dataSetOwner", M.DataSetOwner, ModelType.ACTOR);
		put(type, path + ".dataGenerator", M.DataGenerator, ModelType.ACTOR);
		put(type, path + ".dataDocumentor", M.DataDocumentor, ModelType.ACTOR);
		put(type, path + ".publication", M.Publication, ModelType.SOURCE);
		put(type, path + ".restrictionsDescription", M.AccessAndUseRestrictions);
		put(type, path + ".projectDescription", M.Project);
		put(type, path + ".inventoryMethodDescription", M.LCIMethod);
		put(type, path + ".modelingConstantsDescription", M.ModelingConstants);
		put(type, path + ".completenessDescription", M.DataCompleteness);
		put(type, path + ".dataSelectionDescription", M.DataSelection);
		put(type, path + ".dataTreatmentDescription", M.DataTreatment);
		put(type, path + ".samplingDescription", M.SamplingProcedure);
		put(type, path + ".dataCollectionDescription", M.DataCollectionPeriod);
		put(type, path + ".reviewer", M.Reviewer, ModelType.ACTOR);
		put(type, path + ".reviewDetails", M.DataSetOtherEvaluation);
		put(type, path + ".isCopyrightProtected", M.Copyright);
		put(type, path + ".sources", M.Sources, ModelType.SOURCE);
	}

	private static void putExchangeLabels(ModelType type, String path) {
		put(type, path + ".flow", M.Flow, ModelType.FLOW);
		put(type, path + ".flowProperty", M.FlowProperty, ModelType.FLOW_PROPERTY);
		put(type, path + ".unit", M.Unit, ModelType.UNIT);
		put(type, path + ".amount", M.Amount);
		put(type, path + ".amountFormula", "Amount formula");
		put(type, path + ".isQuantitativeReference", M.QuantitativeReference);
		put(type, path + ".isAvoidedProduct", M.AvoidedProduct);
		put(type, path + ".defaultProvider", M.DefaultProvider);
		put(type, path + ".dqEntry", M.DataQualityEntry);
		put(type, path + ".costCategory", M.CostCategory);
		put(type, path + ".costFormula", M.CostFormula);
		put(type, path + ".costValue", M.CostValue);
		put(type, path + ".currency", M.Currency, ModelType.CURRENCY);
		put(type, path + ".baseUncertainty", M.BaseUncertainty);
		put(type, path + ".location", M.Location, ModelType.LOCATION);
		put(type, path + ".description", M.Description);
		put(type, path + ".uncertainty", M.Uncertainty);
		putUncertaintyLabels(type, path + ".uncertainty");
	}

	private static void putUncertaintyLabels(ModelType type, String path) {
		put(type, path + ".distributionType", M.UncertaintyDistribution);
		put(type, path + ".meanFormula", M.MeanFormula);
		put(type, path + ".mean", M.Mean);
		put(type, path + ".sdFormula", M.StandardDeviationFormula);
		put(type, path + ".sd", M.StandardDeviation);
		put(type, path + ".geomMeanFormula", M.GeometricMeanFormula);
		put(type, path + ".geomMean", M.GeometricMean);
		put(type, path + ".geomSdFormula", M.GeometricStandardDeviationFormula);
		put(type, path + ".geomSd", M.GeometricStandardDeviation);
		put(type, path + ".minimumFormula", M.MinimumFormula);
		put(type, path + ".minimum", M.Minimum);
		put(type, path + ".modeFormula", M.ModeFormula);
		put(type, path + ".mode", M.Mode);
		put(type, path + ".maximumFormula", M.MaximumFormula);
		put(type, path + ".maximum", M.Maximum);
	}

	private static void putAllocationFactorLabels(ModelType type, String path) {
		put(type, path + ".allocationType", M.AllocationMethod);
		put(type, path + ".value", M.Value);
		put(type, path + ".formula", M.Formula);
	}

	private static void putParameterLabels() {
		putParameterLabels(ModelType.PARAMETER, null);
	}

	private static void putParameterLabels(ModelType type, String path) {
		var prefix = path == null ? "" : path + ".";
		putBasicLabels(type, path);
		put(type, prefix + "isInputParameter", M.Type);
		put(type, prefix + "formula", M.Formula);
		put(type, prefix + "value", M.Value);
		put(type, prefix + "uncertainty", M.Uncertainty);
	}

	private static void putSocialAspectLabels(ModelType type, String path) {
		put(type, path + ".rawAmount", M.RawValue);
		put(type, path + ".riskLevel", M.RiskLevel);
		put(type, path + ".activityValue", M.ActivityVariable);
		put(type, path + ".quality", M.DataQuality);
		put(type, path + ".comment", M.Comment);
		put(type, path + ".source", M.Source, ModelType.SOURCE);
	}

	private static void putImpactMethodLabels() {
		var type = ModelType.IMPACT_METHOD;
		putBasicLabels(type);
		put(type, "source", M.Source, ModelType.SOURCE);
		put(type, "code", M.Code);
		put(type, "impactCategories", M.ImpactCategories, ModelType.IMPACT_CATEGORY);
		put(type, "impactCategories.refUnit", M.ReferenceUnit);
		put(type, "nwSets", M.NormalizationWeightingSets);
		putNwSetLabels(type, "nwSets");
	}

	private static void putImpactCategoryLabels() {
		var type = ModelType.IMPACT_CATEGORY;
		putBasicLabels(type);
		put(type, "refUnitName", M.ReferenceUnit);
		put(type, "source", M.Source, ModelType.SOURCE);
		put(type, "code", M.Code);
		put(type, "impactFactors", M.ImpactFactors, ModelType.IMPACT_CATEGORY);
		putImpactFactorLabels(type, "impactFactors");
		put(type, "parameters", M.Parameters, ModelType.PARAMETER);
		putParameterLabels(type, "parameters");
	}

	private static void putImpactFactorLabels(ModelType type, String path) {
		put(type, path + ".flow", M.Flow, ModelType.FLOW);
		put(type, path + ".flowProperty", M.FlowProperty, ModelType.FLOW_PROPERTY);
		put(type, path + ".unit", M.Unit, ModelType.UNIT);
		put(type, path + ".formula", M.Formula);
		put(type, path + ".value", M.Value);
		put(type, path + ".location", M.Location, ModelType.LOCATION);
		put(type, path + ".uncertainty", M.Uncertainty);
		putUncertaintyLabels(type, path + ".uncertainty");
	}

	private static void putNwSetLabels(ModelType type, String path) {
		putBasicLabels(type, path);
		put(type, path + ".weightedScoreUnit", M.ReferenceUnit);
		put(type, path + ".factors", M.Factors, ModelType.IMPACT_CATEGORY);
		putNwFactorLabels(type, path + ".factors");
	}

	private static void putNwFactorLabels(ModelType type, String path) {
		put(type, path + ".normalisationFactor", M.NormalizationFactor);
		put(type, path + ".weightingFactor", M.WeightingFactor);
	}

	private static void putProductSystemLabels() {
		var type = ModelType.PRODUCT_SYSTEM;
		putBasicLabels(type);
		put(type, "refProcess", M.Process, ModelType.PROCESS);
		put(type, "refExchange", M.Product, ModelType.FLOW);
		put(type, "targetFlowProperty", M.FlowProperty, ModelType.FLOW_PROPERTY);
		put(type, "targetUnit", M.Unit, ModelType.UNIT);
		put(type, "targetAmount", M.TargetAmount);
		put(type, "processes", M.Processes, ModelType.PROCESS);
		put(type, "processLinks", M.ProcessLinks);
		put(type, "parameterSets", M.ParameterSets, ModelType.PARAMETER);
		putParameterRedefSetLabels(type, "parameterSets");
	}

	private static void putParameterRedefSetLabels(ModelType type, String path) {
		put(type, path + ".name", M.Name);
		put(type, path + ".description", M.Description);
		put(type, path + ".isBaseline", M.IsBaseline);
		put(type, path + ".parameters", M.Parameters, ModelType.PARAMETER);
		putParameterRedefLabels(type, path + ".parameters");
	}

	private static void putParameterRedefLabels(ModelType type, String path) {
		put(type, path + ".context", M.Context);
		put(type, path + ".name", M.Name);
		put(type, path + ".value", M.Value);
		put(type, path + ".isProtected", M.IsProtected);
		put(type, path + ".uncertainty", M.Uncertainty);
		putUncertaintyLabels(type, path + ".uncertainty");
	}

	private static void putProjectLabels() {
		var type = ModelType.PROJECT;
		putBasicLabels(type);
		put(type, "impactMethod", M.ImpactAssessmentMethod, ModelType.IMPACT_METHOD);
		put(type, "nwSet", M.NormalizationAndWeightingSet);
		put(type, "isWithCosts", M.IsWithCosts);
		put(type, "isWithRegionalization", M.IsWithRegionalization);
		put(type, "variants", M.Variants, ModelType.PRODUCT_SYSTEM);
		putProjectVariantLabels(type, "variants");
	}

	private static void putProjectVariantLabels(ModelType type, String path) {
		put(type, path + ".name", M.Name);
		put(type, path + ".productSystem", M.ProductSystem, ModelType.PRODUCT_SYSTEM);
		put(type, path + ".flowProperty", M.FlowProperty, ModelType.FLOW_PROPERTY);
		put(type, path + ".unit", M.Unit, ModelType.UNIT);
		put(type, path + ".amount", M.Amount);
		put(type, path + ".allocationMethod", M.AllocationMethod);
		put(type, path + ".description", M.Description);
		put(type, path + ".isDisabled", M.IsDisabled);
		put(type, path + ".parameterRedefs", M.Parameters, ModelType.PARAMETER);
		putParameterRedefLabels(type, path + ".parameterRedefs");
	}

	private static void putDQSystemLabels() {
		var type = ModelType.DQ_SYSTEM;
		putBasicLabels(type);
		put(type, "hasUncertainties", M.HasUncertainties);
		put(type, "source", M.Source, ModelType.SOURCE);
		put(type, "indicators", M.Indicators);
		putDQIndicatorLabels(type, "indicators");
	}

	private static void putDQIndicatorLabels(ModelType type, String path) {
		putBasicLabels(type);
		put(type, path + ".scores", M.Scores);
		putDQScoreLabels(type, path + ".scores");
	}

	private static void putDQScoreLabels(ModelType type, String path) {
		putBasicLabels(type);
		put(type, path + ".label", M.Label);
		put(type, path + ".description", M.Description);
		put(type, path + ".uncertainty", M.Uncertainty);
		putUncertaintyLabels(type, path + ".uncertainty");
	}

	private static void putResultLabels() {
		var type = ModelType.RESULT;
		putBasicLabels(type);
		put(type, "productSystem", M.ProductSystem, ModelType.PRODUCT_SYSTEM);
		put(type, "impactMethod", M.ImpactMethod, ModelType.PRODUCT_SYSTEM);
		put(type, "impactResults", M.ImpactResults, ModelType.IMPACT_CATEGORY);
		putImpactResultLabels(type, "impactResults");
		put(type, "inputResults", M.InputResults, ModelType.FLOW);
		putFlowResultLabels(type, "inputResults");
		put(type, "outputResults", M.OutputResults, ModelType.FLOW);
		putFlowResultLabels(type, "outputResults");
	}

	private static void putImpactResultLabels(ModelType type, String path) {
		put(type, path + ".indicator", M.Indicator, ModelType.IMPACT_CATEGORY);
		put(type, path + ".indicator.refUnit", M.ReferenceUnit);
		put(type, path + ".amount", M.Amount);
		put(type, path + ".description", M.Description);
	}

	private static void putFlowResultLabels(ModelType type, String path) {
		put(type, path + ".flow", M.Flow, ModelType.FLOW);
		put(type, path + ".flowProperty", M.FlowProperty, ModelType.FLOW_PROPERTY);
		put(type, path + ".unit", M.Unit, ModelType.UNIT);
		put(type, path + ".location", M.Flow, ModelType.LOCATION);
		put(type, path + ".amount", M.Amount);
		put(type, path + ".description", M.Description);		
	}

	private static void putEpdLabels() {
		var type = ModelType.EPD;
		putBasicLabels(type);
		put(type, "urn", M.Urn);
		put(type, "manufacturer", M.Manufacturer, ModelType.ACTOR);
		put(type, "verifier", M.Verifier, ModelType.ACTOR);
		put(type, "programOperator", M.ProgramOperator, ModelType.ACTOR);
		put(type, "pcr", M.Pcr, ModelType.SOURCE);
		put(type, "product", M.Product, ModelType.FLOW);
		putEpdProduct(type, "product");
		put(type, "modules", M.Modules, ModelType.RESULT);
		put(type, "modules.result", M.Result, ModelType.RESULT);
	}

	private static void putEpdProduct(ModelType type, String path) {
		put(type, path + ".flow", M.Flow, ModelType.FLOW);
		put(type, path + ".flowProperty", M.FlowProperty, ModelType.FLOW_PROPERTY);
		put(type, path + ".unit", M.Unit, ModelType.UNIT);
		put(type, path + ".amount", M.Amount);
	}
	
	private static void put(ModelType type, String path, String label) {
		put(type, path, label, null);
	}

	private static void put(ModelType type, String path, String label, ModelType imageType) {
		var labelMap = labels.get(type);
		if (labelMap == null) {
			labels.put(type, labelMap = new HashMap<>());
		}
		labelMap.put(path, label);
		var ordinalMap = ordinals.get(type);
		if (ordinalMap == null) {
			ordinals.put(type, ordinalMap = new HashMap<>());
		}
		ordinalMap.put(path, ordinalMap.size() + 1);
		if (imageType == null)
			return;
		var typeMap = imageTypes.get(type);
		if (typeMap == null) {
			imageTypes.put(type, typeMap = new HashMap<>());
		}
		typeMap.put(path, imageType);
	}

}
