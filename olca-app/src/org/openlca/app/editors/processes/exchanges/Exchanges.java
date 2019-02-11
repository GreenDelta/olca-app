package org.openlca.app.editors.processes.exchanges;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.core.database.NativeSql;
import org.openlca.core.database.usage.ExchangeUseSearch;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for the exchange tables.
 *
 */
class Exchanges {

	/**
	 * Get the ID of the reference flow of the given descriptor.
	 */
	static long refFlowID(BaseDescriptor d) {
		if (d == null)
			return -1L;

		if (d.type == ModelType.FLOW)
			return d.id;

		String sql = null;
		if (d.type == ModelType.PROCESS) {
			sql = "select e.f_flow from tbl_processes p "
					+ "inner join tbl_exchanges e on "
					+ "p.f_quantitative_reference = e.id "
					+ "where p.id = " + d.id;
		} else if (d.type == ModelType.PRODUCT_SYSTEM) {
			sql = "select e.f_flow from tbl_product_systems s "
					+ "inner join tbl_exchanges e on "
					+ "s.f_reference_exchange = e.id "
					+ "where s.id = " + d.id;
		}
		if (sql == null)
			return -1L;

		try {
			AtomicLong id = new AtomicLong(-1L);
			NativeSql.on(Database.get()).query(sql, r -> {
				id.set(r.getLong(1));
				return false;
			});
			return id.get();
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(Exchanges.class);
			log.error("Failed to query ref. flow: " + sql, e);
			return -1L;
		}
	}

	static boolean canHaveProvider(Exchange e) {
		if (e == null || e.flow == null)
			return false;
		if (e.isInput)
			return e.flow.flowType == FlowType.PRODUCT_FLOW;
		else
			return e.flow.flowType == FlowType.WASTE_FLOW;
	}

	static boolean canRemove(Process process, List<Exchange> exchanges) {
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
