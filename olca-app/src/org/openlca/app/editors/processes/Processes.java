package org.openlca.app.editors.processes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;

/**
 * Some static helper methods for the editor pages in this package.
 */
class Processes {

	private Processes() {
	}

	static List<Exchange> getOutputProducts(Process process) {
		if (process == null)
			return Collections.emptyList();
		List<Exchange> products = new ArrayList<>();
		for (Exchange exchange : process.getExchanges()) {
			if (isOutputProduct(exchange))
				products.add(exchange);
		}
		return products;
	}

	static List<Exchange> getNonOutputProducts(Process process) {
		if (process == null)
			return Collections.emptyList();
		List<Exchange> exchanges = new ArrayList<>();
		for (Exchange exchange : process.getExchanges()) {
			if (isOutputProduct(exchange))
				continue;
			exchanges.add(exchange);
		}
		return exchanges;
	}

	static boolean isOutputProduct(Exchange exchange) {
		return exchange != null
				&& exchange.getFlow() != null
				&& !exchange.isInput()
				&& !exchange.isAvoidedProduct()
				&& exchange.getFlow().getFlowType() == FlowType.PRODUCT_FLOW;
	}

}
