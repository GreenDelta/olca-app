package org.openlca.app.cloud.ui.compare;

import java.util.HashMap;
import java.util.Map;

import org.openlca.app.Messages;
import org.openlca.core.model.Actor;
import org.openlca.core.model.AllocationFactor;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.Category;
import org.openlca.core.model.CostCategory;
import org.openlca.core.model.Currency;
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
		Map<String, String> typeLabels = labels.get(parentType);
		if (typeLabels.containsKey(property))
			return typeLabels.get(property);
		return property;
	}

	static int getOrdinal(String parentType, String property) {
		if (!ordinals.containsKey(parentType))
			return 0;
		Map<String, Integer> typeOrdinals = ordinals.get(parentType);
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
		putCostCategoryLabels();
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
		putProjectLabels();
		putProjectVariantLabels();
	}

	private static void putBasicLabels(Class<?> clazz) {
		put(clazz, "name", Messages.Name);
		put(clazz, "description", Messages.Description);
		if (CategorizedEntity.class.isAssignableFrom(clazz))
			put(clazz, "category", Messages.Category);
	}

	private static void putCategoryLabels() {
		Class<?> clazz = Category.class;
		put(clazz, "modelType", "Model type"); // "#
		put(clazz, "name", Messages.Name);
		put(clazz, "category", Messages.Category);
	}

	private static void putLocationLabels() {
		Class<?> clazz = Location.class;
		putBasicLabels(clazz);
		put(clazz, "code", Messages.Code);
		put(clazz, "longitude", Messages.Longitude);
		put(clazz, "latitude", Messages.Latitude);
		put(clazz, "geometry", "Geography"); // "#
	}

	private static void putCurrencyLabels() {
		Class<?> clazz = Currency.class;
		putBasicLabels(clazz);
		put(clazz, "code", Messages.Code);
		put(clazz, "conversionFactor", Messages.ConversionFactor);
		put(clazz, "referenceCurrency", "Reference currency"); // "#
	}

	private static void putActorLabels() {
		Class<?> clazz = Actor.class;
		putBasicLabels(clazz);
		put(clazz, "address", Messages.Address);
		put(clazz, "city", Messages.City);
		put(clazz, "country", Messages.Country);
		put(clazz, "email", Messages.Email);
		put(clazz, "telefax", Messages.Telefax);
		put(clazz, "telephone", Messages.Telephone);
		put(clazz, "website", Messages.Website);
		put(clazz, "zipCode", Messages.ZipCode);
	}

	private static void putSourceLabels() {
		Class<?> clazz = Source.class;
		putBasicLabels(clazz);
		put(clazz, "doi", Messages.Doi);
		put(clazz, "textReference", Messages.TextReference);
		put(clazz, "year", Messages.Year);
		put(clazz, "externalFile", "File"); // "#
	}

	private static void putUnitGroupLabels() {
		Class<?> clazz = UnitGroup.class;
		putBasicLabels(clazz);
		put(clazz, "defaultFlowProperty", Messages.DefaultFlowProperty);
		put(clazz, "units", Messages.Units);
	}

	private static void putUnitLabels() {
		Class<?> clazz = Unit.class;
		putBasicLabels(clazz);
		put(clazz, "conversionFactor", Messages.ConversionFactor);
		put(clazz, "referenceUnit", Messages.ReferenceUnit);
		put(clazz, "synonyms", Messages.Synonyms);
	}

	private static void putFlowPropertyLabels() {
		Class<?> clazz = FlowProperty.class;
		putBasicLabels(clazz);
		put(clazz, "unitGroup", Messages.UnitGroup);
		put(clazz, "flowPropertyType", Messages.FlowPropertyType);
	}

	private static void putSocialIndicatorLabels() {
		Class<?> clazz = SocialIndicator.class;
		putBasicLabels(clazz);
		put(clazz, "unitOfMeasurement", Messages.UnitOfMeasurement);
		put(clazz, "evaluationScheme", Messages.EvaluationScheme);
		put(clazz, "activityVariable", Messages.ActivityVariable);
		put(clazz, "activityQuantity", "Activity quantity"); // "#
		put(clazz, "activityUnit", "Activity unit"); // "#
	}

	private static void putCostCategoryLabels() {
		Class<?> clazz = CostCategory.class;
		putBasicLabels(clazz);
	}

	private static void putFlowLabels() {
		Class<?> clazz = Flow.class;
		putBasicLabels(clazz);
		put(clazz, "flowType", Messages.FlowType);
		put(clazz, "location", Messages.Location);
		put(clazz, "infrastructureFlow", Messages.InfrastructureFlow);
		put(clazz, "cas", Messages.CASNumber);
		put(clazz, "formula", Messages.Formula);
		put(clazz, "flowProperties", Messages.FlowProperties);
	}

	private static void putFlowPropertyFactorsLabels() {
		Class<?> clazz = FlowPropertyFactor.class;
		put(clazz, "flowProperty", Messages.FlowProperty);
		put(clazz, "conversionFactor", Messages.ConversionFactor);
		put(clazz, "referenceFlowProperty", Messages.ReferenceFlowProperty);
	}

	private static void putProcessLabels() {
		Class<?> clazz = Process.class;
		putBasicLabels(clazz);
		put(clazz, "processType", Messages.ProcessType);
		put(clazz, "location", Messages.Location);
		put(clazz, "infrastructureProcess", Messages.InfrastructureProcess);
		put(clazz, "defaultAllocationMethod", Messages.AllocationMethod);
		put(clazz, "processDocumentation", "Process documentation"); // "#
		put(clazz, "inputs", Messages.Inputs);
		put(clazz, "outputs", Messages.Outputs);
		put(clazz, "allocationFactors", "Allocation factors"); // "#
		put(clazz, "parameters", Messages.Parameters);
		put(clazz, "socialAspects", Messages.SocialAspects);
	}

	private static void putProcessDocumentationLabels() {
		Class<?> clazz = ProcessDocumentation.class;
		put(clazz, "creationDate", Messages.CreationDate);
		put(clazz, "validFrom", Messages.StartDate);
		put(clazz, "validUntil", Messages.EndDate);
		put(clazz, "timeDescription", "Time description"); // "#
		put(clazz, "geographyDescription", "Geography description"); // "#
		put(clazz, "technologyDescription", "Technology description"); // "#
		put(clazz, "intendedApplication", Messages.IntendedApplication);
		put(clazz, "dataSetOwner", Messages.DataSetOwner);
		put(clazz, "dataGenerator", Messages.DataGenerator);
		put(clazz, "dataDocumentor", Messages.DataDocumentor);
		put(clazz, "publication", Messages.Publication);
		put(clazz, "restrictionsDescription", Messages.AccessAndUseRestrictions);
		put(clazz, "projectDescription", Messages.Project);
		put(clazz, "inventoryMethodDescription", Messages.LCIMethod);
		put(clazz, "modelingConstantsDescription", Messages.ModelingConstants);
		put(clazz, "completenessDescription", Messages.DataCompleteness);
		put(clazz, "dataSelectionDescription", Messages.DataSelection);
		put(clazz, "dataTreatmentDescription", Messages.DataTreatment);
		put(clazz, "samplingDescription", Messages.SamplingProcedure);
		put(clazz, "dataCollectionDescription", Messages.DataCollectionPeriod);
		put(clazz, "reviewer", Messages.Reviewer);
		put(clazz, "reviewDetails", Messages.DataSetOtherEvaluation);
		put(clazz, "copyright", Messages.Copyright);
		put(clazz, "sources", Messages.Sources);
	}

	private static void putExchangeLabels() {
		Class<?> clazz = Exchange.class;
		put(clazz, "flow", Messages.Flow);
		put(clazz, "flowProperty", Messages.FlowProperty);
		put(clazz, "unit", Messages.Unit);
		put(clazz, "amount", Messages.Amount);
		put(clazz, "quantitativeReference", Messages.QuantitativeReference);
		put(clazz, "avoidedProduct", Messages.AvoidedProduct);
		put(clazz, "defaultProvider", Messages.DefaultProvider);
		put(clazz, "pedigreeUncertainty", Messages.PedigreeUncertainty);
		put(clazz, "costCategory", Messages.CostCategory);
		put(clazz, "costFormula", "Cost formula"); // "#
		put(clazz, "costValue", "Cost value"); // "#
		put(clazz, "currency", "Currency"); // "#
		put(clazz, "uncertainty", Messages.Uncertainty);
	}

	private static void putUncertaintyLabels() {
		Class<?> clazz = Uncertainty.class;
		put(clazz, "distributionType", Messages.UncertaintyDistribution);
		put(clazz, "meanFormula", "Mean (formula)"); // "#
		put(clazz, "mean", Messages.Mean);
		put(clazz, "sdFormula", "Standard deviation (formula)"); // "#
		put(clazz, "sd", Messages.StandardDeviation);
		put(clazz, "geomMeanFormula", "Geometric mean (formula)"); // "#
		put(clazz, "geomMean", Messages.GeometricMean);
		put(clazz, "geomSdFormula", "Geometric standard deviation (formula)"); // "#
		put(clazz, "geomSd", Messages.GeometricStandardDeviation);
		put(clazz, "minimumFormula", "Minimum (formula)"); // "#
		put(clazz, "minimum", Messages.Minimum);
		put(clazz, "modeFormula", "Mode (formula)"); // "#
		put(clazz, "mode", Messages.Mode);
		put(clazz, "maximumFormula", "Maximum (formula)"); // "#
		put(clazz, "maximum", Messages.Maximum);
	}

	private static void putAllocationFactorLabels() {
		Class<?> clazz = AllocationFactor.class;
		put(clazz, "allocationType", Messages.AllocationMethod);
		put(clazz, "product", Messages.Product);
		put(clazz, "exchange", "Input/Output"); // "#
		put(clazz, "value", Messages.Value);
	}

	private static void putParameterLabels() {
		Class<?> clazz = Parameter.class;
		putBasicLabels(clazz);
		put(clazz, "flowProperty", Messages.FlowProperty);
		put(clazz, "conversionFactor", Messages.ConversionFactor);
		put(clazz, "inputParameter", Messages.Type);
		put(clazz, "formula", Messages.Formula);
		put(clazz, "value", Messages.Value);
	}

	private static void putSocialAspectLabels() {
		Class<?> clazz = SocialAspect.class;
		put(clazz, "socialIndicator", Messages.SocialIndicator);
		put(clazz, "rawAmount", Messages.RawValue);
		put(clazz, "riskLevel", Messages.RiskLevel);
		put(clazz, "activityValue", Messages.ActivityVariable);
		put(clazz, "quality", Messages.DataQuality);
		put(clazz, "comment", Messages.Comment);
		put(clazz, "source", Messages.Source);
	}

	private static void putImpactMethodLabels() {
		Class<?> clazz = ImpactMethod.class;
		putBasicLabels(clazz);
		put(clazz, "impactCategories", Messages.ImpactCategories);
		put(clazz, "nwSets", Messages.NormalizationWeightingSets);
		put(clazz, "parameters", Messages.Parameters);
	}

	private static void putImpactCategoryLabels() {
		Class<?> clazz = ImpactCategory.class;
		putBasicLabels(clazz);
		put(clazz, "referenceUnitName", Messages.ReferenceUnit);
		put(clazz, "impactFactors", Messages.ImpactFactors);
	}

	private static void putImpactFactorLabels() {
		Class<?> clazz = ImpactFactor.class;
		put(clazz, "flow", Messages.Flow);
		put(clazz, "flowProperty", Messages.FlowProperty);
		put(clazz, "unit", Messages.Unit);
		put(clazz, "formula", Messages.Formula);
		put(clazz, "value", Messages.Value);
		put(clazz, "uncertainty", Messages.Uncertainty);
	}

	private static void putNwSetLabels() {
		Class<?> clazz = NwSet.class;
		putBasicLabels(clazz);
		put(clazz, "weightedScoreUnit", Messages.ReferenceUnit);
		put(clazz, "factors", "Factors"); // "#
	}

	private static void putNwFactorLabels() {
		Class<?> clazz = NwFactor.class;
		put(clazz, "impactCategory", Messages.ImpactCategory);
		put(clazz, "normalisationFactor", Messages.NormalizationFactor);
		put(clazz, "weightingFactor", Messages.WeightingFactor);
	}

	private static void putProductSystemLabels() {
		Class<?> clazz = ProductSystem.class;
		putBasicLabels(clazz);
		put(clazz, "referenceProcess", Messages.Process);
		put(clazz, "referenceExchange", Messages.Product);
		put(clazz, "targetFlowProperty", Messages.FlowProperty);
		put(clazz, "targetUnit", Messages.Unit);
		put(clazz, "targetAmount", Messages.TargetAmount);
		put(clazz, "processes", Messages.Processes);
		put(clazz, "processLinks", "Process links"); // "#
		put(clazz, "parameterRedefs", Messages.Parameters);
	}

	private static void putParameterRedefLabels() {
		Class<?> clazz = ParameterRedef.class;
		put(clazz, "context", Messages.Context);
		put(clazz, "name", Messages.Name);
		put(clazz, "value", Messages.Value);
		put(clazz, "uncertainty", Messages.Uncertainty);
	}

	private static void putProjectLabels() {
		Class<?> clazz = Project.class;
		putBasicLabels(clazz);
		put(clazz, "functionalUnit", Messages.FunctionalUnit);
		put(clazz, "goal", Messages.Goal);
		put(clazz, "author", Messages.Author);
		put(clazz, "creationDate", Messages.CreationDate);
		put(clazz, "lastModificationDate", Messages.LastModificationDate);
		put(clazz, "impactMethod", Messages.ImpactAssessmentMethod);
		put(clazz, "nwSet", Messages.NormalizationAndWeightingSet);
		put(clazz, "variants", Messages.Variants);
	}

	private static void putProjectVariantLabels() {
		Class<?> clazz = ProjectVariant.class;
		put(clazz, "name", Messages.Name);
		put(clazz, "productSystem", Messages.ProductSystem);
		put(clazz, "flowProperty", Messages.FlowProperty);
		put(clazz, "unit", Messages.Unit);
		put(clazz, "amount", Messages.Amount);
		put(clazz, "allocationMethod", Messages.AllocationMethod);
		put(clazz, "description", Messages.Description);
		put(clazz, "parameterRedefs", Messages.Parameters);
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
