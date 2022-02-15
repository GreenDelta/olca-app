package org.openlca.app.collaboration.viewers.json.olca;

import java.util.HashMap;
import java.util.Map;

import org.openlca.app.M;
import org.openlca.core.model.Actor;
import org.openlca.core.model.AllocationFactor;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.Category;
import org.openlca.core.model.Currency;
import org.openlca.core.model.DQIndicator;
import org.openlca.core.model.DQScore;
import org.openlca.core.model.DQSystem;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactFactor;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.Location;
import org.openlca.core.model.NwFactor;
import org.openlca.core.model.NwSet;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.ParameterRedefSet;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessDocumentation;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Project;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.SocialAspect;
import org.openlca.core.model.SocialIndicator;
import org.openlca.core.model.Source;
import org.openlca.core.model.Uncertainty;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;

class PropertyLabels {

	private static final Map<String, Map<String, String>> labels = new HashMap<>();
	private static final Map<String, Map<String, Integer>> ordinals = new HashMap<>();

	static {
		putLabels();
	}

	static String get(String parentType, String property) {
		if (!labels.containsKey(parentType))
			return property;
		var typeLabels = labels.get(parentType);
		if (typeLabels.containsKey(property))
			return typeLabels.get(property);
		return property;
	}

	static int getOrdinal(String parentType, String property) {
		if (!ordinals.containsKey(parentType))
			return 0;
		var typeOrdinals = ordinals.get(parentType);
		if (typeOrdinals.containsKey(property))
			return typeOrdinals.get(property);
		return 0;
	}

	private static void putLabels() {
		putCategoryLabels();
		putLocationLabels();
		putCurrencyLabels();
		putActorLabels();
		putSourceLabels();
		putUnitGroupLabels();
		putUnitLabels();
		putFlowPropertyLabels();
		putSocialIndicatorLabels();
		putFlowLabels();
		putFlowPropertyFactorsLabels();
		putProcessLabels();
		putProcessDocumentationLabels();
		putExchangeLabels();
		putUncertaintyLabels();
		putAllocationFactorLabels();
		putParameterLabels();
		putSocialAspectLabels();
		putImpactMethodLabels();
		putImpactCategoryLabels();
		putImpactFactorLabels();
		putNwSetLabels();
		putNwFactorLabels();
		putProductSystemLabels();
		putParameterRedefLabels();
		putParameterRedefSetLabels();
		putProjectLabels();
		putProjectVariantLabels();
		putDQSystemLabels();
		putDQIndicatorLabels();
		putDQScoreLabels();
	}

	private static void putBasicLabels(Class<?> clazz) {
		put(clazz, "name", M.Name);
		put(clazz, "description", M.Description);
		if (CategorizedEntity.class.isAssignableFrom(clazz))
			put(clazz, "category", M.Category);
	}

	private static void putCategoryLabels() {
		var clazz = Category.class;
		put(clazz, "modelType", M.ModelType);
		put(clazz, "name", M.Name);
		put(clazz, "category", M.Category);
	}

	private static void putLocationLabels() {
		var clazz = Location.class;
		putBasicLabels(clazz);
		put(clazz, "code", M.Code);
		put(clazz, "longitude", M.Longitude);
		put(clazz, "latitude", M.Latitude);
		put(clazz, "geometry", M.Geometry);
	}

	private static void putCurrencyLabels() {
		var clazz = Currency.class;
		putBasicLabels(clazz);
		put(clazz, "code", M.Code);
		put(clazz, "conversionFactor", M.ConversionFactor);
		put(clazz, "referenceCurrency", M.ReferenceCurrency);
	}

	private static void putActorLabels() {
		var clazz = Actor.class;
		putBasicLabels(clazz);
		put(clazz, "address", M.Address);
		put(clazz, "city", M.City);
		put(clazz, "country", M.Country);
		put(clazz, "email", M.Email);
		put(clazz, "telefax", M.Telefax);
		put(clazz, "telephone", M.Telephone);
		put(clazz, "website", M.Website);
		put(clazz, "zipCode", M.ZipCode);
	}

	private static void putSourceLabels() {
		var clazz = Source.class;
		putBasicLabels(clazz);
		put(clazz, "url", M.URL);
		put(clazz, "textReference", M.TextReference);
		put(clazz, "year", M.Year);
		put(clazz, "externalFile", M.ExternalFile);
	}

	private static void putUnitGroupLabels() {
		var clazz = UnitGroup.class;
		putBasicLabels(clazz);
		put(clazz, "defaultFlowProperty", M.DefaultFlowProperty);
		put(clazz, "units", M.Units);
	}

	private static void putUnitLabels() {
		var clazz = Unit.class;
		putBasicLabels(clazz);
		put(clazz, "conversionFactor", M.ConversionFactor);
		put(clazz, "referenceUnit", M.ReferenceUnit);
		put(clazz, "synonyms", M.Synonyms);
	}

	private static void putFlowPropertyLabels() {
		var clazz = FlowProperty.class;
		putBasicLabels(clazz);
		put(clazz, "unitGroup", M.UnitGroup);
		put(clazz, "flowPropertyType", M.FlowPropertyType);
	}

	private static void putSocialIndicatorLabels() {
		var clazz = SocialIndicator.class;
		putBasicLabels(clazz);
		put(clazz, "unitOfMeasurement", M.UnitOfMeasurement);
		put(clazz, "evaluationScheme", M.EvaluationSchema);
		put(clazz, "activityVariable", M.ActivityVariable);
		put(clazz, "activityQuantity", M.ActivityQuantity);
		put(clazz, "activityUnit", M.ActivityUnit);
	}

	private static void putFlowLabels() {
		var clazz = Flow.class;
		putBasicLabels(clazz);
		put(clazz, "flowType", M.FlowType);
		put(clazz, "location", M.Location);
		put(clazz, "infrastructureFlow", M.InfrastructureFlow);
		put(clazz, "cas", M.CASNumber);
		put(clazz, "formula", M.Formula);
		put(clazz, "flowProperties", M.FlowProperties);
	}

	private static void putFlowPropertyFactorsLabels() {
		var clazz = FlowPropertyFactor.class;
		put(clazz, "flowProperty", M.FlowProperty);
		put(clazz, "conversionFactor", M.ConversionFactor);
		put(clazz, "referenceFlowProperty", M.ReferenceFlowProperty);
	}

	private static void putProcessLabels() {
		var clazz = Process.class;
		putBasicLabels(clazz);
		put(clazz, "processType", M.ProcessType);
		put(clazz, "location", M.Location);
		put(clazz, "dqSystem", M.ProcessDataQualitySchema);
		put(clazz, "dqEntry", M.DataQualityEntry);
		put(clazz, "exchangeDqSystem", M.ExchangeDataQualitySchema);
		put(clazz, "socialDqSystem", M.SocialDataQualitySchema);
		put(clazz, "infrastructureProcess", M.InfrastructureProcess);
		put(clazz, "defaultAllocationMethod", M.AllocationMethod);
		put(clazz, "processDocumentation", M.ProcessDocumentation);
		put(clazz, "inputs", M.Inputs);
		put(clazz, "outputs", M.Outputs);
		put(clazz, "allocationFactors", M.AllocationFactors);
		put(clazz, "parameters", M.Parameters);
		put(clazz, "socialAspects", M.SocialAspects);
	}

	private static void putProcessDocumentationLabels() {
		var clazz = ProcessDocumentation.class;
		put(clazz, "creationDate", M.CreationDate);
		put(clazz, "validFrom", M.StartDate);
		put(clazz, "validUntil", M.EndDate);
		put(clazz, "timeDescription", M.TimeDescription);
		put(clazz, "geographyDescription", M.GeographyDescription);
		put(clazz, "technologyDescription", M.TechnologyDescription);
		put(clazz, "intendedApplication", M.IntendedApplication);
		put(clazz, "dataSetOwner", M.DataSetOwner);
		put(clazz, "dataGenerator", M.DataGenerator);
		put(clazz, "dataDocumentor", M.DataDocumentor);
		put(clazz, "publication", M.Publication);
		put(clazz, "restrictionsDescription", M.AccessAndUseRestrictions);
		put(clazz, "projectDescription", M.Project);
		put(clazz, "inventoryMethodDescription", M.LCIMethod);
		put(clazz, "modelingConstantsDescription", M.ModelingConstants);
		put(clazz, "completenessDescription", M.DataCompleteness);
		put(clazz, "dataSelectionDescription", M.DataSelection);
		put(clazz, "dataTreatmentDescription", M.DataTreatment);
		put(clazz, "samplingDescription", M.SamplingProcedure);
		put(clazz, "dataCollectionDescription", M.DataCollectionPeriod);
		put(clazz, "reviewer", M.Reviewer);
		put(clazz, "reviewDetails", M.DataSetOtherEvaluation);
		put(clazz, "copyright", M.Copyright);
		put(clazz, "sources", M.Sources);
	}

	private static void putExchangeLabels() {
		var clazz = Exchange.class;
		put(clazz, "flow", M.Flow);
		put(clazz, "flowProperty", M.FlowProperty);
		put(clazz, "unit", M.Unit);
		put(clazz, "amount", M.Amount);
		put(clazz, "quantitativeReference", M.QuantitativeReference);
		put(clazz, "avoidedProduct", M.AvoidedProduct);
		put(clazz, "defaultProvider", M.DefaultProvider);
		put(clazz, "dqEntry", M.DataQualityEntry);
		put(clazz, "costCategory", M.CostCategory);
		put(clazz, "costFormula", M.CostFormula);
		put(clazz, "costValue", M.CostValue);
		put(clazz, "currency", M.Currency);
		put(clazz, "uncertainty", M.Uncertainty);
		put(clazz, "location", M.Location);
	}

	private static void putUncertaintyLabels() {
		var clazz = Uncertainty.class;
		put(clazz, "distributionType", M.UncertaintyDistribution);
		put(clazz, "meanFormula", M.MeanFormula);
		put(clazz, "mean", M.Mean);
		put(clazz, "sdFormula", M.StandardDeviationFormula);
		put(clazz, "sd", M.StandardDeviation);
		put(clazz, "geomMeanFormula", M.GeometricMeanFormula);
		put(clazz, "geomMean", M.GeometricMean);
		put(clazz, "geomSdFormula", M.GeometricStandardDeviationFormula);
		put(clazz, "geomSd", M.GeometricStandardDeviation);
		put(clazz, "minimumFormula", M.MinimumFormula);
		put(clazz, "minimum", M.Minimum);
		put(clazz, "modeFormula", M.ModeFormula);
		put(clazz, "mode", M.Mode);
		put(clazz, "maximumFormula", M.MaximumFormula);
		put(clazz, "maximum", M.Maximum);
	}

	private static void putAllocationFactorLabels() {
		var clazz = AllocationFactor.class;
		put(clazz, "allocationType", M.AllocationMethod);
		put(clazz, "product", M.Product);
		put(clazz, "exchange", M.InputOutput);
		put(clazz, "value", M.Value);
		put(clazz, "formula", M.Formula);
	}

	private static void putParameterLabels() {
		var clazz = Parameter.class;
		putBasicLabels(clazz);
		put(clazz, "flowProperty", M.FlowProperty);
		put(clazz, "conversionFactor", M.ConversionFactor);
		put(clazz, "inputParameter", M.Type);
		put(clazz, "formula", M.Formula);
		put(clazz, "value", M.Value);
	}

	private static void putSocialAspectLabels() {
		var clazz = SocialAspect.class;
		put(clazz, "socialIndicator", M.SocialIndicator);
		put(clazz, "rawAmount", M.RawValue);
		put(clazz, "riskLevel", M.RiskLevel);
		put(clazz, "activityValue", M.ActivityVariable);
		put(clazz, "quality", M.DataQuality);
		put(clazz, "comment", M.Comment);
		put(clazz, "source", M.Source);
	}

	private static void putImpactMethodLabels() {
		var clazz = ImpactMethod.class;
		putBasicLabels(clazz);
		put(clazz, "impactCategories", M.ImpactCategories);
		put(clazz, "nwSets", M.NormalizationWeightingSets);
		put(clazz, "source", M.Source);
		put(clazz, "code", M.Code);
	}

	private static void putImpactCategoryLabels() {
		var clazz = ImpactCategory.class;
		putBasicLabels(clazz);
		put(clazz, "referenceUnitName", M.ReferenceUnit);
		put(clazz, "impactFactors", M.ImpactFactors);
		put(clazz, "source", M.Source);
		put(clazz, "parameters", M.Parameters);
		put(clazz, "code", M.Code);
	}

	private static void putImpactFactorLabels() {
		var clazz = ImpactFactor.class;
		put(clazz, "flow", M.Flow);
		put(clazz, "flowProperty", M.FlowProperty);
		put(clazz, "unit", M.Unit);
		put(clazz, "formula", M.Formula);
		put(clazz, "value", M.Value);
		put(clazz, "location", M.Location);
		put(clazz, "uncertainty", M.Uncertainty);
	}

	private static void putNwSetLabels() {
		var clazz = NwSet.class;
		putBasicLabels(clazz);
		put(clazz, "weightedScoreUnit", M.ReferenceUnit);
		put(clazz, "factors", M.Factors);
	}

	private static void putNwFactorLabels() {
		var clazz = NwFactor.class;
		put(clazz, "impactCategory", M.ImpactCategory);
		put(clazz, "normalisationFactor", M.NormalizationFactor);
		put(clazz, "weightingFactor", M.WeightingFactor);
	}

	private static void putProductSystemLabels() {
		var clazz = ProductSystem.class;
		putBasicLabels(clazz);
		put(clazz, "referenceProcess", M.Process);
		put(clazz, "referenceExchange", M.Product);
		put(clazz, "targetFlowProperty", M.FlowProperty);
		put(clazz, "targetUnit", M.Unit);
		put(clazz, "targetAmount", M.TargetAmount);
		put(clazz, "processes", M.Processes);
		put(clazz, "processLinks", M.ProcessLinks);
		put(clazz, "parameterSets", M.ParameterSets);
	}

	private static void putParameterRedefLabels() {
		var clazz = ParameterRedef.class;
		put(clazz, "context", M.Context);
		put(clazz, "name", M.Name);
		put(clazz, "value", M.Value);
		put(clazz, "uncertainty", M.Uncertainty);
		put(clazz, "isProtected", M.IsProtected);
	}

	private static void putParameterRedefSetLabels() {
		var clazz = ParameterRedefSet.class;
		put(clazz, "name", M.Name);
		put(clazz, "description", M.Description);
		put(clazz, "isBaseline", M.IsBaseline);
		put(clazz, "parameters", M.Parameters);
	}
	
	private static void putProjectLabels() {
		var clazz = Project.class;
		putBasicLabels(clazz);
		put(clazz, "impactMethod", M.ImpactAssessmentMethod);
		put(clazz, "nwSet", M.NormalizationAndWeightingSet);
		put(clazz, "variants", M.Variants);
		put(clazz, "isWithCosts", M.IsWithCosts);
		put(clazz, "isWithRegionalization", M.IsWithRegionalization);
	}

	private static void putProjectVariantLabels() {
		var clazz = ProjectVariant.class;
		put(clazz, "name", M.Name);
		put(clazz, "productSystem", M.ProductSystem);
		put(clazz, "flowProperty", M.FlowProperty);
		put(clazz, "unit", M.Unit);
		put(clazz, "amount", M.Amount);
		put(clazz, "allocationMethod", M.AllocationMethod);
		put(clazz, "description", M.Description);
		put(clazz, "isDisabled", M.IsDisabled);
		put(clazz, "parameterRedefs", M.Parameters);
	}

	private static void putDQSystemLabels() {
		var clazz = DQSystem.class;
		putBasicLabels(clazz);
		put(clazz, "hasUncertainties", M.HasUncertainties);
		put(clazz, "indicators", M.Indicators);
		put(clazz, "source", M.Source);
	}

	private static void putDQIndicatorLabels() {
		var clazz = DQIndicator.class;
		putBasicLabels(clazz);
		put(clazz, "position", M.Position);
		put(clazz, "scores", M.Scores);
	}

	private static void putDQScoreLabels() {
		var clazz = DQScore.class;
		putBasicLabels(clazz);
		put(clazz, "position", M.Position);
		put(clazz, "label", M.Label);
		put(clazz, "description", M.Description);
		put(clazz, "uncertainty", M.Uncertainty);
	}

	private static void put(Class<?> clazz, String property, String label) {
		String type = clazz.getSimpleName();
		Map<String, String> labelMap = labels.get(type);
		if (labelMap == null)
			labels.put(type, labelMap = new HashMap<>());
		labelMap.put(property, label);
		Map<String, Integer> ordinalMap = ordinals.get(type);
		if (ordinalMap == null)
			ordinals.put(type, ordinalMap = new HashMap<>());
		ordinalMap.put(property, ordinalMap.size() + 1);
	}

}
