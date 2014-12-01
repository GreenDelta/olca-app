package org.openlca.app.editors.processes;

import java.util.ArrayList;
import java.util.List;

import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.core.database.usage.ExchangeUseSearch;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.BaseDescriptor;

class Exchanges {

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
		if (process.getQuantitativeReference() == null)
			return false;
		if (exchanges.contains(process.getQuantitativeReference())) {
			org.openlca.app.util.Error.showBox(Messages.CannotDeleteRefFlow,
					Messages.CannotDeleteRefFlowMessage);
			return true;
		}
		return false;
	}

	private static boolean containsUsed(Process process,
			List<Exchange> exchanges) {
		List<Exchange> products = new ArrayList<>();
		for (Exchange exchange : exchanges) {
			Flow flow = exchange.getFlow();
			if (flow != null && flow.getFlowType() != FlowType.ELEMENTARY_FLOW)
				products.add(exchange);
		}
		if (products.isEmpty())
			return false;
		ExchangeUseSearch search = new ExchangeUseSearch(Database.get(),
				process);
		List<BaseDescriptor> list = search.findUses(products);
		if (list.isEmpty())
			return false;
		org.openlca.app.util.Error.showBox("@Cannot remove exchanges",
				"@One or more of the selected exchanges are used already in a "
						+ "product system and therefore cannot be deleted.");
		return true;
	}

}
