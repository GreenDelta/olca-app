package org.openlca.app.editors.graphical_legacy.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.openlca.app.editors.graphical_legacy.model.ExchangeNode;
import org.openlca.app.editors.graphical_legacy.model.ProductSystemNode;
import org.openlca.app.util.Labels;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.NativeSql;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.database.ProductSystemDao;
import org.openlca.core.matrix.index.LongPair;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes the exchange that is already in the product system model and for
 * which we search a link candidates.
 */
class ModelExchange {

	/** The real exchange entity. */
	final Exchange exchange;

	/** The ID of the process to which this exchange belongs. */
	final RootDescriptor process;

	/**
	 * Indicates whether this exchange is already connected. This is only relevant
	 * if this is not a provider flow.
	 */
	final boolean isConnected;

	private ProductSystemNode sysNode;

	ModelExchange(ExchangeNode enode) {
		exchange = enode.exchange;
		process = enode.parent().process;
		sysNode = enode.parent().parent();
		if (isProvider()) {
			isConnected = false;
		} else {
			isConnected = sysNode.linkSearch
					.getConnectionLinks(process.id)
					.stream()
					.anyMatch(link -> link.exchangeId == exchange.id);
		}
	}

	boolean isProvider() {
		boolean isWaste = exchange.flow.flowType == FlowType.WASTE_FLOW;
		return isWaste == exchange.isInput;
	}

	boolean isInput() {
		return exchange.isInput;
	}

	List<Candidate> searchCandidates(IDatabase db) {

		// search for processes that have an exchange
		// with the flow on the opposite side
		List<LongPair> exchanges = new ArrayList<>();
		Set<Long> exchangeIds = new HashSet<>();
		Set<Long> processIds = new HashSet<>();
		try {
			String sql = "SELECT id, f_owner FROM tbl_exchanges "
					+ "WHERE f_flow = " + exchange.flow.id
					+ " AND is_input = " + (exchange.isInput ? 0 : 1);
			NativeSql.on(db).query(sql, r -> {
				long exchangeID = r.getLong("id");
				long processID = r.getLong("f_owner");
				exchanges.add(LongPair.of(
						exchangeID, processID));
				exchangeIds.add(exchangeID);
				processIds.add(processID);
				return true;
			});
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("Error loading connection candidates", e);
		}

		List<ProcessDescriptor> procs = new ProcessDao(db)
				.getDescriptors(processIds);
		Map<Long, RootDescriptor> processes = new HashMap<>();
		for (ProcessDescriptor p : procs) {
			processes.put(p.id, p);
		}

		// add product systems that have the same exchanges as
		// the process candidates as reference flows (these can
		// be connected as sub-systems
		Set<Long> systemIds = new HashSet<Long>();
		try {
			String sql = "SELECT id, f_reference_exchange FROM"
					+ " tbl_product_systems";
			NativeSql.on(db).query(sql, r -> {
				long exchangeID = r.getLong(2);
				if (exchangeIds.contains(exchangeID)) {
					long systemID = r.getLong(1);
					exchanges.add(LongPair.of(exchangeID, systemID));
					systemIds.add(systemID);
				}
				return true;
			});
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("Error loading connection candidates", e);
		}

		if (!systemIds.isEmpty()) {
			new ProductSystemDao(db).getDescriptors(systemIds)
					.forEach(d -> {
						processes.put(d.id, d);
					});
		}

		List<Candidate> candidates = new ArrayList<>();
		for (LongPair e : exchanges) {
			var p = processes.get(e.second());
			if (p == null)
				continue;
			var c = new Candidate(p);
			c.exchangeId = e.first();
			c.processExists = sysNode.getProductSystem().processes.contains(p.id);
			c.isDefaultProvider = !isProvider()
					&& this.exchange.defaultProviderId == p.id;
			if (isProvider()) {
				c.isConnected = sysNode.linkSearch
						.getConnectionLinks(c.process.id)
						.stream()
						.anyMatch(link -> link.exchangeId == c.exchangeId);
			}
			candidates.add(c);
		}

		Collections.sort(candidates);
		return candidates;
	}

	boolean canConnect(Candidate c, List<Candidate> all) {
		if (isProvider())
			return !c.isConnected;
		if (isConnected)
			return false;
		for (Candidate other : all) {
			if (other == c)
				continue;
			if (other.doConnect)
				return false;
		}
		return true;
	}
}

/** Contains the data of a possible connection candidate. */
class Candidate implements Comparable<Candidate> {

	final RootDescriptor process;
	long exchangeId;
	boolean processExists;

	boolean isConnected;
	boolean isDefaultProvider;

	boolean doConnect;
	boolean doCreate;

	Candidate(RootDescriptor process) {
		this.process = process;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Candidate other))
			return false;
		if (other.process == null)
			return process == null;
		return Objects.equals(other.process, process)
				&& exchangeId == other.exchangeId;
	}

	@Override
	public int compareTo(Candidate o) {
		String n1 = Labels.name(process);
		String n2 = Labels.name(o.process);
		return n1.toLowerCase().compareTo(n2.toLowerCase());
	}

}
