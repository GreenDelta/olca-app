package org.openlca.app.tools.transfer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openlca.commons.Res;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.ProviderType;
import org.openlca.core.model.Result;
import org.openlca.core.model.RootEntity;

final class TransferPlanner {

	private TransferPlanner() {
	}

	static Res<TransferPlan> plan(TransferConfig config) {
		if (config == null || config.system() == null) {
			return Res.error("No transfer configuration available");
		}

		var system = config.system();
		if (system.referenceProcess == null) {
			return Res.error("The selected product system has no reference process");
		}

		try {
			var index = ProviderIndex.of(config.target());
			var links = linkIndexOf(system);
			var processCache = new HashMap<Long, Process>();
			var order = new ArrayList<Long>();
			var matches = new LinkedHashMap<ProviderKey, TransferMatch>();
			var queued = new HashSet<Long>();
			var visited = new HashSet<Long>();
			var queue = new ArrayDeque<Long>();
			queue.add(system.referenceProcess.id);
			queued.add(system.referenceProcess.id);

			while (!queue.isEmpty()) {
				long processId = queue.poll();
				queued.remove(processId);
				if (!visited.add(processId))
					continue;

				var process = processOf(config.source(), processId, processCache);
				if (process == null)
					continue;

				order.add(processId);
				for (var exchange : process.exchanges) {
					if (!isLinkableExchange(exchange))
						continue;

					var sourceLink = links.get(exchange.id);
					var sourceProvider = providerOf(config.source(), sourceLink);
					TransferMatch match = null;
					if (sourceProvider != null) {
						var key = new ProviderKey(sourceProvider.type, sourceProvider.id);
						match = matches.computeIfAbsent(key, $ -> {
							var candidates = index.providersOf(exchange.flow.id);
							var selected = defaultMatchOf(index,
								config.strategy(), sourceProvider, exchange.flow.id);
							return new TransferMatch(
								sourceProvider.descriptor,
								candidates,
								selected);
						});
					}

					if ((match == null || match.selectedCandidate() == null)
						&& sourceLink != null
						&& sourceLink.providerType == ProviderType.PROCESS
						&& sourceLink.providerId != 0
						&& !visited.contains(sourceLink.providerId)
						&& !queued.contains(sourceLink.providerId)) {
						queue.add(sourceLink.providerId);
						queued.add(sourceLink.providerId);
					}
				}
			}

			return Res.ok(new TransferPlan(config, order,
				new ArrayList<>(matches.values())));
		} catch (Exception e) {
			return Res.error("Failed to prepare the transfer plan", e);
		}
	}

	private static Map<Long, ProcessLink> linkIndexOf(ProductSystem system) {
		var index = new HashMap<Long, ProcessLink>();
		for (var link : system.processLinks) {
			index.put(link.exchangeId, link);
		}
		return index;
	}

	private static Process processOf(
		IDatabase source,
		long processId,
		Map<Long, Process> cache
	) {
		var cached = cache.get(processId);
		if (cached != null)
			return cached;
		var process = source.get(Process.class, processId);
		if (process != null) {
			cache.put(processId, process);
		}
		return process;
	}

	private static ProviderRef providerOf(IDatabase source, ProcessLink link) {
		if (link == null || link.providerId == 0)
			return null;

		var type = link.providerType;
		Class<? extends RootEntity> providerClass = ProviderType.toModelClass(type);
		var provider = source.get(providerClass, link.providerId);
		if (provider instanceof Process process) {
			return new ProviderRef(
				process.id,
				type,
				Descriptor.of(process),
				process.refId,
				process.name,
				process.location != null ? process.location.code : null);
		}
		if (provider instanceof ProductSystem system) {
			var location = system.referenceProcess != null && system.referenceProcess.location != null
				? system.referenceProcess.location.code
				: null;
			return new ProviderRef(
				system.id, type, Descriptor.of(system), system.refId, system.name, location);
		}
		if (provider instanceof Result result) {
			var location = result.productSystem != null
				&& result.productSystem.referenceProcess != null
				&& result.productSystem.referenceProcess.location != null
					? result.productSystem.referenceProcess.location.code
					: null;
			return new ProviderRef(
				result.id, type, Descriptor.of(result), result.refId, result.name, location);
		}
		return null;
	}

	private static ProviderCandidate defaultMatchOf(
		ProviderIndex index,
		LinkingStrategy strategy,
		ProviderRef sourceProvider,
		long flowId
	) {
		var matches = index.find(
			strategy,
			sourceProvider.refId(),
			sourceProvider.name(),
			sourceProvider.location(),
			flowId);
		return !matches.isEmpty()
			? matches.getFirst()
			: null;
	}

	private static boolean isLinkableExchange(Exchange exchange) {
		if (exchange == null || exchange.flow == null)
			return false;
		var type = exchange.flow.flowType;
		if (type == FlowType.ELEMENTARY_FLOW)
			return false;
		if (type == FlowType.WASTE_FLOW)
			return !exchange.isInput;
		return exchange.isInput;
	}

	private record ProviderRef(
		long id,
		byte type,
		Descriptor descriptor,
		String refId,
		String name,
		String location
	) {
	}

	private record ProviderKey(byte type, long id) {
	}
}
