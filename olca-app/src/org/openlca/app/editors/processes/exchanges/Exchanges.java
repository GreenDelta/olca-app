package org.openlca.app.editors.processes.exchanges;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.NativeSql;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.database.usage.ExchangeUseSearch;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.util.Strings;
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
	static long refFlowID(Descriptor d) {
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

	/**
	 * Checks if the given exchanges can be removed from the process. The exchanges
	 * cannot be removed and a corresponding error message is displayed when:
	 * 
	 * <li>one of the given exchanges is the reference flow of the process
	 * <li>at least one of the exchanges is used in a product system
	 * <li>at least one of the exchanges is needed as default provider link
	 */
	static boolean canRemove(Process p, List<Exchange> exchanges) {
		if (p == null || exchanges == null)
			return false;

		// check reference flow
		if (p.quantitativeReference != null
				&& exchanges.contains(p.quantitativeReference)) {
			MsgBox.error(M.CannotDeleteRefFlow, M.CannotDeleteRefFlowMessage);
			return false;
		}

		// collect product and waste flows
		List<Exchange> techFlows = exchanges.stream()
				.filter(e -> e.flow != null
						&& e.flow.flowType != FlowType.ELEMENTARY_FLOW)
				.collect(Collectors.toList());
		if (techFlows.isEmpty())
			return true;

		// check usage in product systems
		var usages = new ExchangeUseSearch(Database.get(), p).findUses(techFlows);
		if (!usages.isEmpty()) {
			MsgBox.error(M.CannotRemoveExchanges, M.ExchangesAreUsed);
			return false;
		}

		// check provider links
		List<Exchange> providers = techFlows.stream()
				.filter(e -> (e.isInput && e.flow.flowType == FlowType.WASTE_FLOW)
						|| (!e.isInput && e.flow.flowType == FlowType.PRODUCT_FLOW))
				.collect(Collectors.toList());
		if (providers.isEmpty())
			return true;
		for (Exchange provider : providers) {
			String query = "select f_owner from tbl_exchanges where "
					+ "f_default_provider = " + p.id + " and "
					+ "f_flow = " + provider.flow.id + "";
			IDatabase db = Database.get();
			AtomicReference<ProcessDescriptor> ref = new AtomicReference<>();
			try {
				NativeSql.on(db).query(query, r -> {
					long owner = r.getLong(1);
					ProcessDescriptor d = new ProcessDao(db).getDescriptor(owner);
					if (d != null) {
						ref.set(d);
						return false;
					}
					return true;
				});
			} catch (Exception e) {
				Logger log = LoggerFactory.getLogger(Exchanges.class);
				log.error("Failed to query default providers " + query, e);
				return false;
			}
			if (ref.get() == null)
				continue;

			// we found an usage as default provider, now we need to make sure
			// that there is no other exchange with the same flow and direction
			// that can fulfill this role (and that is not in the list of
			// exchanges to be deleted).
			boolean ok = p.exchanges.stream().filter(e -> e.id != provider.id
					&& e.isInput == provider.isInput
					&& e.flow != null
					&& e.flow.id == provider.flow.id
					&& !exchanges.contains(e)).findAny().isPresent();
			if (ok)
				continue;

			MsgBox.error("Flow used as default provider",
					"This process is linked as default provider with flow `"
							+ Strings.cut(Labels.name(provider.flow), 75)
							+ "` in process `"
							+ Strings.cut(Labels.name(ref.get()), 75)
							+ "`.");
			return false;
		}

		return true;
	}

}
