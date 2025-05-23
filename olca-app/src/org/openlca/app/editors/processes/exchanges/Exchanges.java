package org.openlca.app.editors.processes.exchanges;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
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
 */
public class Exchanges {

	/// Get the ID of the reference flow of the given descriptor. If the
	/// descriptor is itself a flow, it returns the ID of that flow.
	static long refFlowIdOf(Descriptor d) {
		if (d == null)
			return -1L;
		if (d.type == ModelType.FLOW)
			return d.id;

		var sql = switch (d.type) {
			case ModelType.PROCESS -> "select e.f_flow from"
					+ " tbl_processes p inner join tbl_exchanges e on"
					+ " p.f_quantitative_reference = e.id"
					+ " where p.id = " + d.id;
			case ModelType.PRODUCT_SYSTEM -> "select e.f_flow from"
					+ " tbl_product_systems s inner join tbl_exchanges e on "
					+ " s.f_reference_exchange = e.id"
					+ " where s.id = " + d.id;
			case ModelType.RESULT -> "select e.f_flow from"
					+ " tbl_results r inner join tbl_flow_results e on "
					+ " r.f_reference_flow = e.id"
					+ " where r.id = " + d.id;
			case null, default -> null;
		};
		if (sql == null)
			return -1L;

		try {
			var id = new AtomicLong(-1L);
			NativeSql.on(Database.get()).query(sql, r -> {
				id.set(r.getLong(1));
				return false;
			});
			return id.get();
		} catch (Exception e) {
			ErrorReporter.on("Failed to query ref. flow: " + sql, e);
			return -1L;
		}
	}

	static boolean canHaveProvider(Exchange e, Descriptor d) {
		if (e == null || e.flow == null || d == null)
			return false;
		if (d.type != ModelType.PROCESS && d.type != ModelType.RESULT)
			return false;
		return e.isInput
			? e.flow.flowType == FlowType.PRODUCT_FLOW
			: e.flow.flowType == FlowType.WASTE_FLOW;
	}

	/**
	 * Checks if the given exchanges can be removed from the process. The exchanges
	 * cannot be removed and a corresponding error message is displayed when:
	 *
	 * <li>one of the given exchanges is the reference flow of the process
	 * <li>at least one of the exchanges is used in a product system
	 * <li>at least one of the exchanges is needed as default provider link
	 */
	public static boolean canRemove(Process p, List<Exchange> exchanges) {
		if (p == null || exchanges == null)
			return false;

		if (!checkRefFlow(p, exchanges))
			return false;

		// collect product and waste flows
		List<Exchange> techFlows = exchanges.stream()
				.filter(e -> e.flow != null && e.flow.flowType != FlowType.ELEMENTARY_FLOW)
				.collect(Collectors.toList());
		if (techFlows.isEmpty())
			return true;

		if (!checkSystemUsage(p, techFlows))
			return false;

		return checkProviderLinks(p, exchanges, techFlows);
	}

	public static boolean checkProviderLinks(
			Process p, List<Exchange> exchanges, List<Exchange> techFlows
	) {
		List<Exchange> providers = techFlows.stream()
				.filter(e -> (e.isInput && e.flow.flowType == FlowType.WASTE_FLOW)
						|| (!e.isInput && e.flow.flowType == FlowType.PRODUCT_FLOW))
				.toList();
		if (providers.isEmpty())
			return true;
		for (Exchange provider : providers) {
			var query = "select f_owner from tbl_exchanges where "
					+ "f_default_provider = " + p.id + " and "
					+ "f_flow = " + provider.flow.id;
			var db = Database.get();
			var ref = new AtomicReference<ProcessDescriptor>();
			try {
				NativeSql.on(db).query(query, r -> {
					long owner = r.getLong(1);
					var d = new ProcessDao(db).getDescriptor(owner);
					if (d != null) {
						ref.set(d);
						return false;
					}
					return true;
				});
			} catch (Exception e) {
				Logger log = LoggerFactory.getLogger(Exchanges.class);
				log.error("Failed to query default providers: {}", query, e);
				return false;
			}
			if (ref.get() == null)
				continue;

			// we found a usage as default provider, now we need to make sure
			// that there is no other exchange with the same flow and direction
			// that can fulfill this role (and that is not in the list of
			// exchanges to be deleted).
			boolean ok = p.exchanges.stream().anyMatch(e ->
					e.id != provider.id
							&& e.isInput == provider.isInput
							&& e.flow != null
							&& e.flow.id == provider.flow.id
							&& !exchanges.contains(e));
			if (ok)
				continue;

			MsgBox.error(M.FlowUsedAsDefaultProvider, M.FlowUsedAsDefaultProviderErr
					+ "\r\n " + Strings.cut(Labels.name(provider.flow), 75)
					+ "\r\n " + Strings.cut(Labels.name(ref.get()), 75));
			return false;
		}
		return true;
	}

	public static boolean checkSystemUsage(Process p, List<Exchange> techFlows) {
		var usages = new ExchangeUseSearch(Database.get(), p).findUses(techFlows);
		if (!usages.isEmpty()) {
			MsgBox.error(M.CannotRemoveExchanges, M.ExchangesAreUsed);
			return false;
		} else return true;
	}

	public static boolean checkRefFlow(Process p, List<Exchange> exchanges) {
		if (p.quantitativeReference != null
				&& exchanges.contains(p.quantitativeReference)) {
			MsgBox.error(M.CannotDeleteRefFlow, M.CannotDeleteRefFlowMessage);
			return false;
		} else return true;
	}

}
