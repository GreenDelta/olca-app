package org.openlca.app.util;

import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.core.database.CurrencyDao;
import org.openlca.core.math.data_quality.AggregationType;
import org.openlca.core.math.data_quality.NAHandling;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.matrix.linking.ProviderLinking;
import org.openlca.core.model.Process;
import org.openlca.core.model.*;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.model.descriptors.LocationDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.io.CategoryPath;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.RoundingMode;
import java.util.Objects;

public class Labels {

	private Labels() {
	}

	public static String name(RefEntity entity) {
		if (entity == null || entity.name == null)
			return "";
		if (entity instanceof Flow flow)
			return LocationCode.append(flow.name, flow.location);
		if (entity instanceof Process process)
			return LocationCode.append(process.name, process.location);
		if (entity instanceof Location location)
			return LocationCode.append(location.name, location.code);
		return entity.name;
	}

	public static String name(Descriptor d) {
		if (d == null || d.name == null)
			return "";
		var cache = Cache.getEntityCache();
		String text = d.name;
		if (cache == null)
			return text;
		if (d instanceof ProcessDescriptor process)
			return LocationCode.append(process.name, process.location);
		if (d instanceof FlowDescriptor flow)
			return LocationCode.append(flow.name, flow.location);
		if (d instanceof LocationDescriptor location)
			return LocationCode.append(location.name, location.code);
		return text;
	}

	public static String name(TechFlow product) {
		// currently we just return the process name but
		// in future versions we could also return
		// process-flow pairs here. though this could
		// result in very long display names.
		return product == null
				? ""
				: name(product.provider());
	}

	public static String name(EnviFlow flow) {
		if (flow == null || flow.flow() == null)
			return "";
		if (flow.isVirtual() && flow.wrapped() != null)
			return name(flow.wrapped());
		if (flow.flow().flowType != FlowType.ELEMENTARY_FLOW)
			return Labels.name(flow.flow());
		return flow.location() != null
				? LocationCode.append(flow.flow().name, flow.location().code)
				: flow.flow().name;
	}

	public static String refUnit(EnviFlow flow) {
		if (flow == null)
			return "";
		if (flow.isVirtual() && flow.wrapped() instanceof ImpactDescriptor i)
			return i.referenceUnit;
		return refUnit(flow.flow());
	}

	public static String refUnit(TechFlow flow) {
		return flow == null
				? ""
				: refUnit(flow.flow());
	}

	public static String refUnit(FlowDescriptor flow) {
		if (flow == null)
			return "";
		FlowProperty refProp = Cache.getEntityCache().get(
				FlowProperty.class,
				flow.refFlowPropertyId);
		if (refProp == null)
			return "";
		UnitGroup unitGroup = refProp.unitGroup;
		if (unitGroup == null)
			return "";
		Unit unit = unitGroup.referenceUnit;
		if (unit == null)
			return "";
		return unit.name;
	}

	public static String category(EnviFlow flow) {
		if (flow == null || flow.flow() == null)
			return "";
		return category(flow.flow());
	}

	public static String category(RootEntity e) {
		if (e == null || e.category == null)
			return "";
		return CategoryPath.getFull(e.category);
	}

	/**
	 * Returns the full category path of the given entity.
	 */
	public static String category(RootDescriptor d) {
		if (d == null || d.category == null)
			return "";
		Category c = Cache.getEntityCache().get(Category.class, d.category);
		if (c == null)
			return "";
		return CategoryPath.getFull(c);
	}

	public static String category(TechFlow techFlow) {
		return techFlow != null
				? category(techFlow.provider())
				: "";
	}

	public static String code(Location loc) {
		if (loc == null)
			return "";
		return Strings.notEmpty(loc.code)
				? loc.code
				: loc.name;
	}

	public static String getEnumText(Object enumValue) {
		if (enumValue instanceof AllocationMethod)
			return Labels.of((AllocationMethod) enumValue);
		if (enumValue instanceof FlowPropertyType)
			return Labels.of((FlowPropertyType) enumValue);
		if (enumValue instanceof FlowType)
			return Labels.of((FlowType) enumValue);
		if (enumValue instanceof ProcessType)
			return Labels.of((ProcessType) enumValue);
		if (enumValue instanceof UncertaintyType)
			return Labels.of((UncertaintyType) enumValue);
		if (enumValue instanceof RiskLevel)
			return Labels.of((RiskLevel) enumValue);
		if (enumValue instanceof ModelType)
			return Labels.of((ModelType) enumValue);
		if (enumValue != null)
			return enumValue.toString();
		return null;
	}

	/**
	 * Returns the label for the given uncertainty distribution type. If the
	 * given type is NULL the value for 'no distribution' is returned.
	 */
	public static String of(UncertaintyType type) {
		if (type == null)
			return M.NoDistribution;
		return switch (type) {
			case LOG_NORMAL -> M.LogNormalDistribution;
			case NORMAL -> M.NormalDistribution;
			case TRIANGLE -> M.TriangleDistribution;
			case UNIFORM -> M.UniformDistribution;
			default -> M.NoDistribution;
		};
	}

	public static String of(FlowType type) {
		if (type == null)
			return null;
		return switch (type) {
			case ELEMENTARY_FLOW -> M.ElementaryFlow;
			case PRODUCT_FLOW -> M.Product;
			case WASTE_FLOW -> M.Waste;
		};
	}

	public static String of(ProcessType t) {
		if (t == null)
			return null;
		return switch (t) {
			case LCI_RESULT -> M.SystemProcess;
			case UNIT_PROCESS -> M.UnitProcess;
		};
	}

	public static String of(AllocationMethod m) {
		if (m == null)
			return null;
		return switch (m) {
			case CAUSAL -> M.Causal;
			case ECONOMIC -> M.Economic;
			case PHYSICAL -> M.Physical;
			case USE_DEFAULT -> M.AsDefinedInProcesses;
			default -> M.None;
		};
	}

	public static String of(FlowPropertyType t) {
		if (t == null)
			return null;
		return switch (t) {
			case ECONOMIC -> M.Economic;
			case PHYSICAL -> M.Physical;
		};
	}

	public static String of(RiskLevel rl) {
		if (rl == null)
			return M.Unknown;
		return switch (rl) {
			case HIGH_OPPORTUNITY -> M.HighOpportunity;
			case MEDIUM_OPPORTUNITY -> M.MediumOpportunity;
			case LOW_OPPORTUNITY -> M.LowOpportunity;
			case NO_RISK -> M.NoRisk;
			case VERY_LOW_RISK -> M.VeryLowRisk;
			case LOW_RISK -> M.LowRisk;
			case MEDIUM_RISK -> M.MediumRisk;
			case HIGH_RISK -> M.HighRisk;
			case VERY_HIGH_RISK -> M.VeryHighRisk;
			case NO_DATA -> M.NoData;
			case NOT_APPLICABLE -> M.NotApplicable;
			case NO_OPPORTUNITY -> M.NoOpportunity;
		};
	}

	public static String plural(ModelType o) {
		if (o == null)
			return null;
		return switch (o) {
			case ACTOR -> M.Actors;
			case CURRENCY -> M.Currencies;
			case FLOW -> M.Flows;
			case FLOW_PROPERTY -> M.FlowProperties;
			case IMPACT_METHOD -> M.ImpactAssessmentMethods;
			case IMPACT_CATEGORY -> M.ImpactCategories;
			case PROCESS -> M.Processes;
			case PRODUCT_SYSTEM -> M.ProductSystems;
			case PROJECT -> M.Projects;
			case SOCIAL_INDICATOR -> M.SocialIndicators;
			case SOURCE -> M.Sources;
			case UNIT_GROUP -> M.UnitGroups;
			case LOCATION -> M.Locations;
			case PARAMETER -> M.GlobalParameters;
			case CATEGORY -> M.Category;
			case DQ_SYSTEM -> M.DataQualitySystems;
			case RESULT -> M.Results;
			case EPD -> "EPDs";
		};
	}

	public static String of(ModelType o) {
		if (o == null)
			return null;
		return switch (o) {
			case ACTOR -> M.Actor;
			case CURRENCY -> M.Currency;
			case FLOW -> M.Flow;
			case FLOW_PROPERTY -> M.FlowProperty;
			case IMPACT_METHOD -> M.ImpactAssessmentMethod;
			case IMPACT_CATEGORY -> M.ImpactCategory;
			case PROCESS -> M.Process;
			case PRODUCT_SYSTEM -> M.ProductSystem;
			case PROJECT -> M.Project;
			case SOCIAL_INDICATOR -> M.SocialIndicator;
			case SOURCE -> M.Source;
			case UNIT_GROUP -> M.UnitGroup;
			case LOCATION -> M.Location;
			case PARAMETER -> M.GlobalParameter;
			case CATEGORY -> M.Category;
			case RESULT -> M.Result;
			case EPD -> "EPD";
			default -> M.Unknown;
		};
	}

	public static String of(AggregationType type) {
		if (type == null)
			return null;
		return switch (type) {
			case WEIGHTED_AVERAGE -> M.WeightedAverage;
			case WEIGHTED_SQUARED_AVERAGE -> M.WeightedSquaredAverage;
			case MAXIMUM -> M.Maximum;
			case NONE -> M.None;
		};
	}

	public static String of(NAHandling type) {
		if (type == null)
			return null;
		return switch (type) {
			case EXCLUDE -> M.ExcludeZeroValues;
			case USE_MAX -> M.UseMaximumScoreForZeroValues;
		};
	}

	public static String of(RoundingMode mode) {
		if (mode == null)
			return "?";
		return switch (mode) {
			case HALF_UP -> M.HalfUp;
			case CEILING -> M.Up;
			// TODO: we may should add labels for the other modes too, but these are
			// currently the only modes that we use in the DQI aggregation
			default -> "?";
		};
	}

	public static String of(ProviderLinking providerLinking) {
		if (providerLinking == null)
			return M.Unknown;
		return switch (providerLinking) {
			case IGNORE_DEFAULTS -> M.IgnoreDefaultProviders;
			case ONLY_DEFAULTS -> M.OnlyLinkDefaultProviders;
			case PREFER_DEFAULTS -> M.PreferDefaultProviders;
		};
	}

	public static String getReferenceCurrencyCode() {
		try {
			CurrencyDao dao = new CurrencyDao(Database.get());
			Currency c = dao.getReferenceCurrency();
			if (c != null && c.code != null)
				return c.code;
			else
				return "?";
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(Labels.class);
			log.error("failed to get reference currency", e);
			return "?";
		}
	}

	private static class LocationCode {

		static String append(String name, Long locationId) {
			if (locationId == null)
				return name;
			var cache = Cache.getEntityCache();
			if (cache == null)
				return name;
			var location = cache.get(LocationDescriptor.class, locationId);
			return location != null
					? append(name, location.code)
					: name;
		}

		static String append(String name, Location location) {
			return location != null
					? append(name, location.code)
					: name;
		}

		static String append(String name, String code) {
			return Strings.nullOrEmpty(code) || Objects.equals(name, code)
					? name
					: name + " - " + code;
		}

	}

}
