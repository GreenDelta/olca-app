package org.openlca.app.editors.processes.allocation;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openlca.core.model.AllocationFactor;
import org.openlca.core.model.AllocationMethod;
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
		return p.exchanges.stream()
			.filter(Util::isProvider)
			.collect(Collectors.toList());
	}

	/**
	 * Non-provider flows are product inputs, waste outputs and all elementary
	 * flows that are partitioned when applying allocation factors to create
	 * mono-functional processes.
	 */
	static List<Exchange> getNonProviderFlows(Process p) {
		if (p == null)
			return Collections.emptyList();
		return p.exchanges.stream()
			.filter(e -> !isProvider(e))
			.collect(Collectors.toList());
	}

	private static boolean isProvider(Exchange e) {
		if (e == null || e.flow == null || e.isAvoided)
			return false;
		FlowType type = e.flow.flowType;
		if (type == FlowType.PRODUCT_FLOW && !e.isInput)
			return true;
		return type == FlowType.WASTE_FLOW && e.isInput;
	}

	/**
	 * Returns the causal factor for the given product output or waste input and
	 * exchange. Returns {@code null} if no such factor exists.
	 */
	static AllocationFactor factorOf(
		Process process, Exchange product, Exchange exchange) {
		if (product == null
			|| product.flow == null
			|| exchange == null)
			return null;
		for (var factor : process.allocationFactors) {
			if (factor.method != AllocationMethod.CAUSAL)
				continue;
			if (product.flow.id != factor.productId)
				continue;
			if (!Objects.equals(factor.exchange, exchange))
				continue;
			return factor;
		}
		return null;
	}
}
