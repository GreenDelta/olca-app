package org.openlca.app.util;

import java.math.RoundingMode;

import org.apache.commons.lang3.tuple.Pair;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.core.database.CurrencyDao;
import org.openlca.core.database.EntityCache;
import org.openlca.core.math.data_quality.AggregationType;
import org.openlca.core.math.data_quality.NAHandling;
import org.openlca.core.matrix.IndexFlow;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.Category;
import org.openlca.core.model.Currency;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyType;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Location;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.RiskLevel;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.UncertaintyType;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.io.CategoryPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class Labels {

	private Labels() {
	}

	public static String name(RootEntity entity) {
		if (entity == null || entity.name == null)
			return "";
		Location loc = null;
		if (entity instanceof Flow) {
			Flow flow = (Flow) entity;
			loc = flow.location;
		}
		if (entity instanceof Process) {
			Process process = (Process) entity;
			loc = process.location;
		}
		if (loc == null || Strings.isNullOrEmpty(loc.code))
			return entity.name;
		return entity.name + " - " + loc.code;
	}

	public static String name(Descriptor d) {
		if (d == null)
			return "";
		EntityCache cache = Cache.getEntityCache();
		String text = d.name;
		if (cache == null)
			return text;
		Long locationId = null;
		if (d instanceof ProcessDescriptor) {
			ProcessDescriptor process = (ProcessDescriptor) d;
			locationId = process.location;
		}
		if (d instanceof FlowDescriptor) {
			FlowDescriptor flow = (FlowDescriptor) d;
			locationId = flow.location;
		}
		if (locationId != null) {
			Location loc = cache.get(Location.class, locationId);
			if (loc != null && !Strings.isNullOrEmpty(loc.code))
				text = text + " - " + loc.code;
		}
		return text;
	}

	public static String name(IndexFlow flow) {
		if (flow == null || flow.flow == null)
			return "";
		if (flow.flow.flowType != FlowType.ELEMENTARY_FLOW)
			return Labels.name(flow.flow);
		String name = flow.flow.name;
		if (flow.location != null) {
			name += " - " + flow.location.code;
		}
		return name;
	}

	public static String refUnit(IndexFlow flow) {
		if (flow == null)
			return "";
		return refUnit(flow.flow);
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

	public static String category(IndexFlow flow) {
		if (flow == null || flow.flow == null)
			return "";
		return category(flow.flow);
	}

	public static String category(CategorizedEntity e) {
		if (e == null || e.category == null)
			return "";
		return CategoryPath.getFull(e.category);
	}

	/**
	 * Returns the full category path of the given entity.
	 */
	public static String category(CategorizedDescriptor d) {
		if (d == null || d.category == null)
			return "";
		Category c = Cache.getEntityCache().get(Category.class, d.category);
		if (c == null)
			return "";
		return CategoryPath.getFull(c);
	}

	/**
	 * We often have to show the category and sub-category of a flow in the
	 * result pages. This method returns a pair where the left value is the
	 * category and the right value is the sub-category. Default values are
	 * empty strings.
	 */
	public static Pair<String, String> getCategory(CategorizedDescriptor entity) {
		EntityCache cache = Cache.getEntityCache();
		if (entity == null || entity.category == null)
			return Pair.of("", "");
		Category cat = cache.get(Category.class, entity.category);
		if (cat == null)
			return Pair.of("", "");
		if (cat.category == null)
			return Pair.of(cat.name, "");
		else
			return Pair.of(cat.category.name, cat.name);
	}

	/**
	 * Same as {@link #getCategory(CategorizedDescriptor, EntityCache)} but top-
	 * and sub-category concatenated as a short string.
	 */
	public static String getShortCategory(CategorizedDescriptor entity) {
		Pair<String, String> p = getCategory(entity);
		if (Strings.isNullOrEmpty(p.getLeft()) && Strings.isNullOrEmpty(p.getRight()))
			return "";
		if (Strings.isNullOrEmpty(p.getLeft()))
			return p.getRight();
		if (Strings.isNullOrEmpty(p.getRight()))
			return p.getLeft();
		return p.getLeft() + " / " + p.getRight();
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
		switch (type) {
		case LOG_NORMAL:
			return M.LogNormalDistribution;
		case NONE:
			return M.NoDistribution;
		case NORMAL:
			return M.NormalDistribution;
		case TRIANGLE:
			return M.TriangleDistribution;
		case UNIFORM:
			return M.UniformDistribution;
		default:
			return M.NoDistribution;
		}
	}

	public static String of(FlowType type) {
		if (type == null)
			return null;
		switch (type) {
		case ELEMENTARY_FLOW:
			return M.ElementaryFlow;
		case PRODUCT_FLOW:
			return M.Product;
		case WASTE_FLOW:
			return M.Waste;
		default:
			return null;
		}
	}

	public static String of(ProcessType t) {
		if (t == null)
			return null;
		switch (t) {
		case LCI_RESULT:
			return M.SystemProcess;
		case UNIT_PROCESS:
			return M.UnitProcess;
		default:
			return null;
		}
	}

	public static String of(AllocationMethod m) {
		if (m == null)
			return null;
		switch (m) {
		case CAUSAL:
			return M.Causal;
		case ECONOMIC:
			return M.Economic;
		case NONE:
			return M.None;
		case PHYSICAL:
			return M.Physical;
		case USE_DEFAULT:
			return M.AsDefinedInProcesses;
		default:
			return M.None;
		}
	}

	public static String of(FlowPropertyType t) {
		if (t == null)
			return null;
		switch (t) {
		case ECONOMIC:
			return M.Economic;
		case PHYSICAL:
			return M.Physical;
		default:
			return null;
		}
	}

	public static String of(RiskLevel rl) {
		if (rl == null)
			return M.Unknown;
		switch (rl) {
		case HIGH_OPPORTUNITY:
			return M.HighOpportunity;
		case MEDIUM_OPPORTUNITY:
			return M.MediumOpportunity;
		case LOW_OPPORTUNITY:
			return M.LowOpportunity;
		case NO_RISK:
			return M.NoRisk;
		case VERY_LOW_RISK:
			return M.VeryLowRisk;
		case LOW_RISK:
			return M.LowRisk;
		case MEDIUM_RISK:
			return M.MediumRisk;
		case HIGH_RISK:
			return M.HighRisk;
		case VERY_HIGH_RISK:
			return M.VeryHighRisk;
		case NO_DATA:
			return M.NoData;
		case NOT_APPLICABLE:
			return M.NotApplicable;
		case NO_OPPORTUNITY:
			return M.NoOpportunity;
		default:
			return M.Unknown;
		}
	}

	public static String plural(ModelType o) {
		if (o == null)
			return null;
		switch (o) {
		case ACTOR:
			return M.Actors;
		case CURRENCY:
			return M.Currencies;
		case FLOW:
			return M.Flows;
		case FLOW_PROPERTY:
			return M.FlowProperties;
		case IMPACT_METHOD:
			return M.ImpactAssessmentMethods;
		case IMPACT_CATEGORY:
			return M.ImpactCategories;
		case PROCESS:
			return M.Processes;
		case PRODUCT_SYSTEM:
			return M.ProductSystems;
		case PROJECT:
			return M.Projects;
		case SOCIAL_INDICATOR:
			return M.SocialIndicators;
		case SOURCE:
			return M.Sources;
		case UNIT_GROUP:
			return M.UnitGroups;
		case LOCATION:
			return M.Locations;
		case PARAMETER:
			return M.GlobalParameters;
		case CATEGORY:
			return M.Category;
		case DQ_SYSTEM:
			return M.DataQualitySystems;
		default:
			return M.Unknown;
		}
	}

	public static String of(ModelType o) {
		if (o == null)
			return null;
		switch (o) {
		case ACTOR:
			return M.Actor;
		case CURRENCY:
			return M.Currency;
		case FLOW:
			return M.Flow;
		case FLOW_PROPERTY:
			return M.FlowProperty;
		case IMPACT_METHOD:
			return M.ImpactAssessmentMethod;
		case IMPACT_CATEGORY:
			return M.ImpactCategory;
		case PROCESS:
			return M.Process;
		case PRODUCT_SYSTEM:
			return M.ProductSystem;
		case PROJECT:
			return M.Project;
		case SOCIAL_INDICATOR:
			return M.SocialIndicator;
		case SOURCE:
			return M.Source;
		case UNIT_GROUP:
			return M.UnitGroup;
		case LOCATION:
			return M.Location;
		case PARAMETER:
			return M.GlobalParameter;
		case CATEGORY:
			return M.Category;
		default:
			return M.Unknown;
		}
	}

	public static String of(AggregationType type) {
		if (type == null)
			return null;
		switch (type) {
		case WEIGHTED_AVERAGE:
			return M.WeightedAverage;
		case WEIGHTED_SQUARED_AVERAGE:
			return M.WeightedSquaredAverage;
		case MAXIMUM:
			return M.Maximum;
		case NONE:
			return M.None;
		default:
			return null;
		}
	}

	public static String of(NAHandling type) {
		if (type == null)
			return null;
		switch (type) {
		case EXCLUDE:
			return M.ExcludeZeroValues;
		case USE_MAX:
			return M.UseMaximumScoreForZeroValues;
		default:
			return null;
		}
	}

	public static String of(RoundingMode mode) {
		if (mode == null)
			return null;
		switch (mode) {
		case HALF_UP:
			return M.HalfUp;
		case CEILING:
			return M.Up;
		default:
			return null;
		}
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

}
