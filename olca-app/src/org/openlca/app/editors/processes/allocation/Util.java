package org.openlca.app.editors.processes.allocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;

class Util {

	/**
	 * Provider flows are product outputs are waste inputs. For each of such a
	 * flow a mono-functional process can be created when applying allocation
	 * factors.
	 */
	static List<Exchange> getProviderFlows(Process p) {
		if (p == null)
			return Collections.emptyList();
		List<Exchange> list = new ArrayList<>();
		for (Exchange e : p.exchanges) {
			if (isProvider(e))
				list.add(e);
		}
		return list;
	}

	/**
	 * Non-provider flows are product inputs, waste outputs and all elementary
	 * flows that are partitioned when applying allocation factors to create
	 * mono-functional processes.
	 */
	static List<Exchange> getNonProviderFlows(Process p) {
		if (p == null)
			return Collections.emptyList();
		List<Exchange> list = new ArrayList<>();
		for (Exchange e : p.exchanges) {
			if (isProvider(e))
				continue;
			list.add(e);
		}
		return list;
	}

	private static boolean isProvider(Exchange e) {
		if (e == null || e.flow == null || e.isAvoided)
			return false;
		FlowType type = e.flow.flowType;
		if (type == FlowType.PRODUCT_FLOW && !e.isInput)
			return true;
		if (type == FlowType.WASTE_FLOW && e.isInput)
			return true;
		return false;
	}
}
