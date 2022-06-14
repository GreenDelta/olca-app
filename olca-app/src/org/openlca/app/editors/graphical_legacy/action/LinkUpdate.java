package org.openlca.app.editors.graphical_legacy.action;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Objects;

import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.NativeSql;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.matrix.ProductSystemBuilder;
import org.openlca.core.matrix.linking.ProviderLinking;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;

public class LinkUpdate {

	private final IDatabase db;
	private final ProductSystem system;

	// linking properties
	private final ProviderLinking providerLinking;
	private final ProcessType preferredType;
	private final boolean keepExisting;
	private final boolean preferSameLocations;

	// indices
	// flowId -> flow descriptor
	private final TLongObjectMap<FlowDescriptor> flows;
	// processId -> process descriptor
	private final TLongObjectMap<ProcessDescriptor> processes;
	// exchangeId -> process link
	private final TLongObjectMap<ProcessLink> oldLinks;
	// flowId -> process Ids
	private final TLongObjectMap<TLongSet> providerIndex;
	// processId -> exchange Ids
	private final TLongObjectMap<TLongSet> exchangeIndex;
	// exchangeId -> exchange
	private final TLongObjectMap<TExchange> exchanges;

	private LinkUpdate(Config config) {
		db = config.db;
		system = config.system;
		providerLinking = config.providerLinking;
		preferredType = config.preferredType;
		keepExisting = config.keepExisting;
		preferSameLocations = config.preferSameLocations;

		flows = new FlowDao(db).descriptorMap();
		processes = new ProcessDao(db).descriptorMap();
		oldLinks = new TLongObjectHashMap<>();
		providerIndex = new TLongObjectHashMap<>();
		exchangeIndex = new TLongObjectHashMap<>();
		exchanges = new TLongObjectHashMap<>();
	}

	public static Config of(IDatabase db, ProductSystem system) {
		return new Config(db, system);
	}

	private ProductSystem execute() {
		if (system.referenceProcess == null)
			return system;

		// initialize the data structures
		fillIndices();
		var newLinks = new ArrayList<ProcessLink>();
		var handled = new TLongHashSet();
		var queue = new ArrayDeque<Long>();
		queue.add(system.referenceProcess.id);

		// traverse the graph in breadth first order
		while (!queue.isEmpty()) {

			long processId = queue.poll();
			var process = processes.get(processId);
			if (process == null)
				continue;

			var exchangeIds = exchangeIndex.get(processId);
			if (exchangeIds == null)
				continue;
			for (var it = exchangeIds.iterator(); it.hasNext();) {
				var link = linkOf(process, it.next());
				if (link == null)
					continue;
				newLinks.add(link);
				var provider = link.providerId;
				if (!handled.contains(provider)) {
					handled.add(provider);
					queue.add(provider);
				}
			}
		}

		// update the links and processes
		system.processLinks.clear();
		system.processLinks.addAll(newLinks);
		system.processes.clear();
		for (var link : newLinks) {
			system.processes.add(link.processId);
			system.processes.add(link.providerId);
		}

		return ProductSystemBuilder.update(db, system);
	}

	private ProcessLink linkOf(ProcessDescriptor process, long exchangeId) {
		if (keepExisting) {
			var old = oldLinks.get(exchangeId);
			if (old != null)
				return old;
		}

		var exchange = exchanges.get(exchangeId);
		if (exchange == null)
			return null;
		var providers = providerIndex.get(exchange.flowId);
		if (providers == null)
			return null;

		long providerId = 0;
		ProcessDescriptor provider = null;
		for (var it = providers.iterator(); it.hasNext();) {
			long candidateId = it.next();

			// only link default providers
			if (providerLinking == ProviderLinking.ONLY_DEFAULTS) {
				if (candidateId == exchange.defaultProviderId) {
					providerId = candidateId;
					break;
				}
				continue;
			}

			// set the candidate as provider if there is nothing better
			var candidate = processes.get(candidateId);
			if (candidate == null)
				continue;
			if (isBetterProvider(process, provider, candidate, exchange)) {
				provider = candidate;
				providerId = candidateId;
			}
		}

		if (providerId == 0)
			return null;
		var link = new ProcessLink();
		link.providerId = providerId;
		link.exchangeId = exchangeId;
		link.processId = process.id;
		link.providerType = ProcessLink.ProviderType.PROCESS;
		link.flowId = exchange.flowId;
		return link;
	}

	/**
	 * Returns true when the given candidate is a better provider then the currently
	 * selected provider
	 */
	private boolean isBetterProvider(
			ProcessDescriptor process,
			ProcessDescriptor current,
			ProcessDescriptor candidate,
			TExchange exchange) {
		if (current == null)
			return true;

		// prefer same locations
		if (preferSameLocations && process.location != null) {
			// TODO: we could filter out 'GLO' and 'RoW' here
			long locationId = process.location;
			boolean matchesCurrent = current.location != null
					&& locationId == current.location;
			boolean matchesCandidate = candidate.location != null
					&& locationId == candidate.location;
			if (!matchesCurrent && matchesCandidate
					&& candidate.id != process.id)
				return true;
		}

		// prefer default providers
		if (providerLinking == ProviderLinking.PREFER_DEFAULTS) {
			if (current.id == exchange.defaultProviderId)
				return false;
			if (candidate.id == exchange.defaultProviderId)
				return true;
		}

		// check the preferred process type
		return candidate.processType == preferredType
				&& current.processType != preferredType;
	}

	private void fillIndices() {
		// index the current links if needed
		if (keepExisting) {
			for (var oldLink : system.processLinks) {
				oldLinks.put(oldLink.exchangeId, oldLink);
			}
		}

		// collect providers and exchange data
		var sql = "select " +
		/* 1 */ "id, " +
		/* 2 */ "f_owner, " +
		/* 3 */ "f_flow, " +
		/* 4 */ "is_input, " +
		/* 5 */ "f_default_provider from tbl_exchanges";

		NativeSql.on(db).query(sql, r -> {

			// check the flow type
			long flowId = r.getLong(3);
			var flow = flows.get(flowId);
			if (flow == null
					|| flow.flowType == null
					|| flow.flowType == FlowType.ELEMENTARY_FLOW)
				return true;

			// check if this is a provider flow
			long ownerId = r.getLong(2);
			if (!processes.containsKey(ownerId))
				return true;
			boolean isInput = r.getBoolean(4);
			if ((isInput && flow.flowType == FlowType.WASTE_FLOW)
					|| (!isInput && flow.flowType == FlowType.PRODUCT_FLOW)) {
				add(providerIndex, flowId, ownerId);
				return true;
			}

			// add as linkable exchange
			long exchangeId = r.getLong(1);
			long defaultProviderId = r.getLong(5);
			var exchange = new TExchange(flowId, defaultProviderId);
			add(exchangeIndex, ownerId, exchangeId);
			exchanges.put(exchangeId, exchange);
			return true;
		});
	}

	private void add(TLongObjectMap<TLongSet> map, long key, long val) {
		var set = map.get(key);
		if (set == null) {
			set = new TLongHashSet();
			map.put(key, set);
		}
		set.add(val);
	}

	private static class TExchange {

		final long flowId;
		final long defaultProviderId;

		TExchange(long flowId, long defaultProviderId) {
			this.flowId = flowId;
			this.defaultProviderId = defaultProviderId;
		}
	}

	public static class Config {

		private final IDatabase db;
		private final ProductSystem system;
		private ProviderLinking providerLinking = ProviderLinking.PREFER_DEFAULTS;
		private ProcessType preferredType = ProcessType.LCI_RESULT;
		private boolean keepExisting;
		private boolean preferSameLocations;

		private Config(IDatabase db, ProductSystem system) {
			this.db = Objects.requireNonNull(db);
			this.system = Objects.requireNonNull(system);
		}

		public Config withProviderLinking(ProviderLinking providerLinking) {
			if (providerLinking != null) {
				this.providerLinking = providerLinking;
			}
			return this;
		}

		public Config withPreferredType(ProcessType type) {
			if (type != null) {
				this.preferredType = type;
			}
			return this;
		}

		public Config keepExistingLinks(boolean b) {
			this.keepExisting = b;
			return this;
		}

		public Config preferLinksInSameLocation(boolean b) {
			this.preferSameLocations = b;
			return this;
		}

		public ProductSystem execute() {
			return new LinkUpdate(this).execute();
		}
	}

}
