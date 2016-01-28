package org.openlca.app.util;

import org.apache.commons.lang3.tuple.Pair;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.core.database.CurrencyDao;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.AllocationMethod;
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
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Labels {

	private Labels() {
	}

	public static String getDisplayName(RootEntity entity) {
		if (entity == null || entity.getName() == null)
			return "";
		if (entity instanceof Flow) {
			Flow flow = (Flow) entity;
			Location location = flow.getLocation();
			if (location != null && location.getCode() != null) {
				return flow.getName() + " - " + location.getCode();
			}
		}
		if (entity instanceof Process) {
			Process process = (Process) entity;
			Location location = process.getLocation();
			if (location != null && location.getCode() != null) {
				return process.getName() + " - " + location.getCode();
			}
		}
		return entity.getName();
	}

	public static String getDisplayName(BaseDescriptor descriptor) {
		if (descriptor == null)
			return "";
		EntityCache cache = Cache.getEntityCache();
		String text = descriptor.getName();
		if (cache == null)
			return text;
		Long locationId = null;
		if (descriptor instanceof ProcessDescriptor) {
			ProcessDescriptor process = (ProcessDescriptor) descriptor;
			locationId = process.getLocation();
		}
		if (descriptor instanceof FlowDescriptor) {
			FlowDescriptor flow = (FlowDescriptor) descriptor;
			locationId = flow.getLocation();
		}
		if (locationId != null) {
			Location location = cache.get(Location.class, locationId);
			if (location != null && location.getCode() != null)
				text = text + " - " + location.getCode();
		}
		return text;
	}

	public static String getDisplayInfoText(BaseDescriptor descriptor) {
		if (descriptor == null)
			return "";
		return descriptor.getDescription();
	}

	public static String getRefUnit(FlowDescriptor flow, EntityCache cache) {
		if (flow == null)
			return "";
		FlowProperty refProp = cache.get(FlowProperty.class,
				flow.getRefFlowPropertyId());
		if (refProp == null)
			return "";
		UnitGroup unitGroup = refProp.getUnitGroup();
		if (unitGroup == null)
			return "";
		Unit unit = unitGroup.getReferenceUnit();
		if (unit == null)
			return "";
		return unit.getName();
	}

	/**
	 * We often have to show the category and sub-category of a flow in the
	 * result pages. This method returns a pair where the left value is the
	 * category and the right value is the sub-category. Default values are
	 * empty strings.
	 */
	public static Pair<String, String> getFlowCategory(FlowDescriptor flow,
			EntityCache cache) {
		if (flow == null || flow.getCategory() == null)
			return Pair.of("", "");
		Category cat = cache.get(Category.class, flow.getCategory());
		if (cat == null)
			return Pair.of("", "");
		if (cat.getCategory() == null)
			return Pair.of(cat.getName(), "");
		else
			return Pair.of(cat.getCategory().getName(), cat.getName());
	}

	public static String getEnumText(Object enumValue) {
		if (enumValue instanceof AllocationMethod)
			return Labels.allocationMethod((AllocationMethod) enumValue);
		if (enumValue instanceof FlowPropertyType)
			return Labels.flowPropertyType((FlowPropertyType) enumValue);
		if (enumValue instanceof FlowType)
			return Labels.flowType((FlowType) enumValue);
		if (enumValue instanceof ProcessType)
			return Labels.processType((ProcessType) enumValue);
		if (enumValue instanceof UncertaintyType)
			return Labels.uncertaintyType((UncertaintyType) enumValue);
		if (enumValue instanceof RiskLevel)
			return Labels.riskLevel((RiskLevel) enumValue);
		if (enumValue instanceof ModelType)
			return Labels.modelTypeSingular((ModelType) enumValue);
		if (enumValue != null)
			return enumValue.toString();
		return null;
	}

	/**
	 * Returns the label for the given uncertainty distribution type. If the
	 * given type is NULL the value for 'no distribution' is returned.
	 */
	public static String uncertaintyType(UncertaintyType type) {
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

	public static String flowType(Flow flow) {
		if (flow == null)
			return null;
		return flowType(flow.getFlowType());
	}

	public static String flowType(FlowType type) {
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

	public static String processType(Process process) {
		if (process == null)
			return null;
		return processType(process.getProcessType());
	}

	public static String processType(ProcessType processType) {
		if (processType == null)
			return null;
		switch (processType) {
		case LCI_RESULT:
			return M.SystemProcess;
		case UNIT_PROCESS:
			return M.UnitProcess;
		default:
			return null;
		}
	}

	public static String allocationMethod(AllocationMethod allocationMethod) {
		if (allocationMethod == null)
			return null;
		switch (allocationMethod) {
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

	public static String flowPropertyType(FlowProperty property) {
		if (property == null)
			return null;
		return flowPropertyType(property.getFlowPropertyType());
	}

	public static String flowPropertyType(FlowPropertyType type) {
		if (type == null)
			return null;
		switch (type) {
		case ECONOMIC:
			return M.Economic;
		case PHYSICAL:
			return M.Physical;
		default:
			return null;
		}
	}

	public static String riskLevel(RiskLevel rl) {
		if (rl == null)
			return M.Unknown;
		// "# TODO: these values should be translated
		switch (rl) {
		case HIGH_OPPORTUNITY:
			return "High opportunity";
		case MEDIUM_OPPORTUNITY:
			return "Medium opportunity";
		case LOW_OPPORTUNITY:
			return "Low opportunity";
		case NO_RISK:
			return "No risk";
		case VERY_LOW_RISK:
			return "Very low risk";
		case LOW_RISK:
			return "Low risk";
		case MEDIUM_RISK:
			return "Medium risk";
		case HIGH_RISK:
			return "High risk";
		case VERY_HIGH_RISK:
			return "Very high risk";
		case NO_DATA:
			return "No data";
		case NOT_APPLICABLE:
			return "Not applicable";
		default:
			return M.Unknown;
		}
	}

	public static String modelType(ModelType o) {
		if (o == null)
			return null;
		switch (o) {
		case ACTOR:
			return M.Actors;
		case CURRENCY:
			return "#Currencies";
		case FLOW:
			return M.Flows;
		case FLOW_PROPERTY:
			return M.FlowProperties;
		case IMPACT_METHOD:
			return M.ImpactAssessmentMethods;
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
		default:
			return M.Unknown;
		}
	}

	public static String modelTypeSingular(ModelType o) {
		if (o == null)
			return null;
		switch (o) {
		case ACTOR:
			return M.Actor;
		case CURRENCY:
			return "#Currency";
		case FLOW:
			return M.Flow;
		case FLOW_PROPERTY:
			return M.FlowProperty;
		case IMPACT_METHOD:
			return M.ImpactAssessmentMethod;
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
			return "#Global parameter";
		case CATEGORY:
			return M.Category;
		default:
			return M.Unknown;
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
