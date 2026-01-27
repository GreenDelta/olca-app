package org.openlca.app.editors.graphical.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import org.openlca.app.editors.graphical.model.ExchangeItem;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.NativeSql;
import org.openlca.core.matrix.index.LongPair;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes the exchange that is already in the product system model and for
 * which we search a link candidates.
 */
class ModelExchange {

	/// The real exchange entity.
	final Exchange exchange;

	/// The ID of the process to which this exchange belongs.
	final RootDescriptor process;

	/// Indicates whether this exchange is a provider flow, i.e. a flow that
	/// is a product output or waste input of the process.
	final boolean isProvider;

	/// Indicates whether this exchange is already connected. This is only
	/// relevant if this is not a provider flow.
	final boolean isConnected;

	final Graph graph;

	ModelExchange(ExchangeItem item) {
		exchange = item.exchange;
		process = item.getNode().descriptor;
		graph = item.getGraph();

		isProvider = exchange.flow != null &&
				exchange.flow.flowType != null &&
				switch (exchange.flow.flowType) {
					case PRODUCT_FLOW -> !exchange.isInput;
					case WASTE_FLOW -> exchange.isInput;
					default -> false;
				};

		isConnected = !isProvider &&
				graph.linkSearch
						.getConsumerLinks(process.id)
						.stream()
						.anyMatch(link -> link.exchangeId == exchange.id);
	}

	boolean isInput() {
		return exchange.isInput;
	}

	List<LinkCandidate> searchLinkCandidates(IDatabase db) {

		// search for processes that have an exchange
		// with the flow on the opposite side
		var exchanges = new ArrayList<LongPair>();
		var exchangeIds = new HashSet<Long>();
		var processIds = new HashSet<Long>();
		try {
			String sql = "SELECT id, f_owner FROM tbl_exchanges "
					+ "WHERE f_flow = " + exchange.flow.id
					+ " AND is_input = " + (exchange.isInput ? 0 : 1);
			NativeSql.on(db).query(sql, r -> {
				long exchangeId = r.getLong("id");
				long processId = r.getLong("f_owner");
				exchanges.add(LongPair.of(exchangeId, processId));
				exchangeIds.add(exchangeId);
				processIds.add(processId);
				return true;
			});
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("Error loading connection candidates", e);
		}

		var processes = new HashMap<Long, RootDescriptor>();
		for (var p : db.getDescriptors(Process.class, processIds)) {
			processes.put(p.id, p);
		}

		// add product systems that have the same exchanges as
		// the process candidates as reference flows (these can
		// be connected as sub-systems
		var systemIds = new HashSet<Long>();
		try {
			String sql = "SELECT id, f_reference_exchange FROM"
					+ " tbl_product_systems";
			NativeSql.on(db).query(sql, r -> {
				long exchangeId = r.getLong(2);
				if (!exchangeIds.contains(exchangeId))
					return true;
				long systemId = r.getLong(1);
				if (systemId != graph.getProductSystem().id) {
					exchanges.add(LongPair.of(exchangeId, systemId));
					systemIds.add(systemId);
				}
				return true;
			});
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("Error loading connection candidates", e);
		}

		if (!systemIds.isEmpty()) {
			db.getDescriptors(ProductSystem.class, systemIds)
					.forEach(d -> processes.put(d.id, d));
		}

		var candidates = new ArrayList<LinkCandidate>();
		for (LongPair e : exchanges) {
			var p = processes.get(e.second());
			if (p == null)
				continue;
			candidates.add(LinkCandidate.of(this, p, e.first()));
		}

		Collections.sort(candidates);
		return candidates;
	}

	boolean canConnect(LinkCandidate c, List<LinkCandidate> all) {
		if (isProvider)
			return !c.isConnected;
		if (isConnected)
			return false;
		for (LinkCandidate other : all) {
			if (Objects.equals(other, c))
				continue;
			if (other.doConnect || other.doCreate)
				return false;
		}
		return true;
	}

	public boolean canBeAdded(LinkCandidate c, List<LinkCandidate> all) {
		for (LinkCandidate other : all) {
			if (Objects.equals(other, c))
				continue;
			if (other.doCreate || other.doConnect)
				return false;
		}
		return true;
	}

}

