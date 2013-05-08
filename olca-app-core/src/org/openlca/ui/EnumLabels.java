package org.openlca.ui;

import org.openlca.core.application.Messages;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyType;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.UncertaintyDistributionType;

public class EnumLabels {

	private EnumLabels() {
	}

	/**
	 * Returns the label for the given uncertainty distribution type. If the
	 * given type is NULL the value for 'no distribution' is returned.
	 */
	public static String uncertaintyType(UncertaintyDistributionType type) {
		if (type == null)
			return Messages.Common_NoDistribution;
		switch (type) {
		case LOG_NORMAL:
			return Messages.Common_LogNormalDistribution;
		case NONE:
			return Messages.Common_NoDistribution;
		case NORMAL:
			return Messages.Common_NormalDistribution;
		case TRIANGLE:
			return Messages.Common_TriangleDistribution;
		case UNIFORM:
			return Messages.Common_UniformDistribution;
		default:
			return Messages.Common_NoDistribution;
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
		case ElementaryFlow:
			return Messages.Common_ElementaryFlow;
		case ProductFlow:
			return Messages.Common_ProductFlow;
		case WasteFlow:
			return Messages.Common_WasteFlow;
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
		case LCI_Result:
			return Messages.Common_SystemProcess;
		case UnitProcess:
			return Messages.Common_UnitProcess;
		default:
			return null;
		}
	}

	public static String allocationMethod(Process process) {
		if (process == null)
			return Messages.Common_None;
		return allocationMethod(process.getAllocationMethod());
	}

	public static String allocationMethod(AllocationMethod allocationMethod) {
		if (allocationMethod == null)
			return null;
		switch (allocationMethod) {
		case Causal:
			return Messages.Common_Causal;
		case Economic:
			return Messages.Common_Economic;
		case None:
			return Messages.Common_None;
		case Physical:
			return Messages.Common_Physical;
		default:
			return Messages.Common_None;
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
		case Economic:
			return Messages.Common_Economic;
		case Physical:
			return Messages.Common_Physical;
		default:
			return null;
		}
	}

}
