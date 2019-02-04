package org.openlca.app.editors.processes.exchanges;

import java.util.ArrayList;
import java.util.List;

import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.core.database.usage.ExchangeUseSearch;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.CategorizedDescriptor;

class Exchanges {

	public static boolean canHaveProvider(Exchange e) {
		if (e == null || e.flow == null)
			return false;
		if (e.isInput)
			return e.flow.flowType == FlowType.PRODUCT_FLOW;
		else
			return e.flow.flowType == FlowType.WASTE_FLOW;
	}

	public static boolean canRemove(Process process, List<Exchange> exchanges) {
		if (process == null || exchanges == null)
			return false;
		if (containsRefFlow(process, exchanges))
			return false;
		if (containsUsed(process, exchanges))
			return false;
		else
			return true;
	}

	private static boolean containsRefFlow(Process process,
			List<Exchange> exchanges) {
		if (process.quantitativeReference == null)
			return false;
		if (exchanges.contains(process.quantitativeReference)) {
			org.openlca.app.util.Error.showBox(M.CannotDeleteRefFlow,
					M.CannotDeleteRefFlowMessage);
			return true;
		}
		return false;
	}

	private static boolean containsUsed(Process process,
			List<Exchange> exchanges) {
		List<Exchange> products = new ArrayList<>();
		for (Exchange exchange : exchanges) {
			Flow flow = exchange.flow;
			if (flow != null && flow.flowType != FlowType.ELEMENTARY_FLOW)
				products.add(exchange);
		}
		if (products.isEmpty())
			return false;
		ExchangeUseSearch search = new ExchangeUseSearch(Database.get(),
				process);
		List<CategorizedDescriptor> list = search.findUses(products);
		if (list.isEmpty())
			return false;
		org.openlca.app.util.Error.showBox(M.CannotRemoveExchanges,
				M.ExchangesAreUsed);
		return true;
	}

}
