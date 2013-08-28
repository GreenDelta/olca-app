package org.openlca.app.util;

import org.openlca.app.Messages;
import org.openlca.core.database.Cache;
import org.openlca.core.database.DatabaseContent;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyType;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.UncertaintyDistributionType;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;

public class Labels {

	private Labels() {
	}

	public static String getDisplayName(BaseDescriptor descriptor) {
		// TODO: location infos for processes & flows
		if (descriptor == null)
			return "";
		return descriptor.getName();
	}

	public static String getDisplayInfoText(BaseDescriptor descriptor) {
		if (descriptor == null)
			return "";
		return descriptor.getDescription();
	}

	public static String getRefUnit(FlowDescriptor flow, Cache cache) {
		if (flow == null)
			return "";
		FlowProperty refProp = cache.getFlowProperty(flow
				.getRefFlowPropertyId());
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

	public static String getEnumText(Object enumValue) {
		if (enumValue instanceof AllocationMethod)
			return Labels.allocationMethod((AllocationMethod) enumValue);
		if (enumValue instanceof FlowPropertyType)
			return Labels.flowPropertyType((FlowPropertyType) enumValue);
		if (enumValue instanceof FlowType)
			return Labels.flowType((FlowType) enumValue);
		if (enumValue instanceof ProcessType)
			return Labels.processType((ProcessType) enumValue);
		if (enumValue instanceof UncertaintyDistributionType)
			return Labels
					.uncertaintyType((UncertaintyDistributionType) enumValue);
		if (enumValue != null)
			return enumValue.toString();
		return null;
	}

	/**
	 * Returns the label for the given uncertainty distribution type. If the
	 * given type is NULL the value for 'no distribution' is returned.
	 */
	public static String uncertaintyType(UncertaintyDistributionType type) {
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
			return Messages.ProductFlow;
		case WASTE_FLOW:
			return Messages.WasteFlow;
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

	public static String databaseContent(DatabaseContent content) {
		if (content == null)
			return null;
		switch (content) {
		case EMPTY:
			return Messages.EmptyDatabase;
		case UNITS:
			return Messages.UnitsAndFlowProps;
		case ALL_REF_DATA:
			return Messages.CompleteRefData;
		default:
			return null;
		}
	}
}
