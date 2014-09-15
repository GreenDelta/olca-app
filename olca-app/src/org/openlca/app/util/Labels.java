package org.openlca.app.util;

import org.apache.commons.lang3.tuple.Pair;
import org.openlca.app.Messages;
import org.openlca.app.db.Cache;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.Category;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyType;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Location;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.UncertaintyType;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;

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
		if (cat.getParentCategory() == null)
			return Pair.of(cat.getName(), "");
		else
			return Pair.of(cat.getParentCategory().getName(), cat.getName());
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
			return Messages.NoDistribution;
		switch (type) {
		case LOG_NORMAL:
			return Messages.LogNormalDistribution;
		case NONE:
			return Messages.NoDistribution;
		case NORMAL:
			return Messages.NormalDistribution;
		case TRIANGLE:
			return Messages.TriangleDistribution;
		case UNIFORM:
			return Messages.UniformDistribution;
		default:
			return Messages.NoDistribution;
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
			return Messages.ElementaryFlow;
		case PRODUCT_FLOW:
			return Messages.Product;
		case WASTE_FLOW:
			return Messages.Waste;
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
			return Messages.SystemProcess;
		case UNIT_PROCESS:
			return Messages.UnitProcess;
		default:
			return null;
		}
	}

	public static String allocationMethod(AllocationMethod allocationMethod) {
		if (allocationMethod == null)
			return null;
		switch (allocationMethod) {
		case CAUSAL:
			return Messages.Causal;
		case ECONOMIC:
			return Messages.Economic;
		case NONE:
			return Messages.None;
		case PHYSICAL:
			return Messages.Physical;
		case USE_DEFAULT:
			return Messages.AsDefinedInProcesses;
		default:
			return Messages.None;
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
			return Messages.Economic;
		case PHYSICAL:
			return Messages.Physical;
		default:
			return null;
		}
	}

}
