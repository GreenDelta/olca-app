package org.openlca.app.tools.transfer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Result;

/// A minimal target-side index of provider candidates.
final class ProviderIndex {

	private final Map<Long, List<ProviderCandidate>> byFlow = new HashMap<>();
	private final Map<String, List<ProviderCandidate>> byRefId = new HashMap<>();
	private final Map<NameAndLocation, List<ProviderCandidate>> byNameAndLocation = new HashMap<>();

	static ProviderIndex of(IDatabase db) {
		var index = new ProviderIndex();
		if (db == null)
			return index;

		for (var descriptor : db.getDescriptors(Process.class)) {
			var process = db.get(Process.class, descriptor.id);
			if (process == null)
				continue;
			for (var exchange : process.exchanges) {
				if (!isProviderExchange(exchange))
					continue;
				index.add(ProviderCandidate.of(process, exchange));
			}
		}

		for (var descriptor : db.getDescriptors(ProductSystem.class)) {
			index.add(ProviderCandidate.of(db.get(ProductSystem.class, descriptor.id)));
		}

		for (var descriptor : db.getDescriptors(Result.class)) {
			index.add(ProviderCandidate.of(db.get(Result.class, descriptor.id)));
		}

		return index;
	}

	void add(ProviderCandidate candidate) {
		if (candidate == null)
			return;

		put(byFlow, candidate.flowId(), candidate);
		if (candidate.providerRefId() != null) {
			put(byRefId, candidate.providerRefId(), candidate);
		}
		put(byNameAndLocation,
			new NameAndLocation(candidate.providerName(), candidate.providerLocation()),
			candidate);
	}

	List<ProviderCandidate> providersOf(long flowId) {
		var providers = byFlow.get(flowId);
		return providers != null
			? List.copyOf(providers)
			: List.of();
	}

	List<ProviderCandidate> find(
		LinkingStrategy strategy,
		String refId,
		String name,
		String location,
		long flowId
	) {
		if (strategy == null)
			return List.of();

		var byId = filter(byRefId.get(refId), flowId);
		return switch (strategy) {
			case BY_ID -> byId;
			case BY_NAME -> merge(byId,
				filter(byNameAndLocation.get(new NameAndLocation(name, location)), flowId));
		};
	}

	private static boolean isProviderExchange(Exchange exchange) {
		if (exchange == null || exchange.flow == null)
			return false;
		var type = exchange.flow.flowType;
		if (type == FlowType.ELEMENTARY_FLOW)
			return false;
		if (type == FlowType.WASTE_FLOW)
			return exchange.isInput;
		return !exchange.isInput;
	}

	private static <K> void put(
		Map<K, List<ProviderCandidate>> index,
		K key,
		ProviderCandidate candidate
	) {
		index.computeIfAbsent(key, $ -> new ArrayList<>()).add(candidate);
	}

	private static List<ProviderCandidate> filter(
		List<ProviderCandidate> candidates,
		long flowId
	) {
		if (candidates == null || candidates.isEmpty())
			return List.of();
		var matches = new ArrayList<ProviderCandidate>();
		for (var candidate : candidates) {
			if (candidate.hasFlow(flowId)) {
				matches.add(candidate);
			}
		}
		return matches;
	}

	private static List<ProviderCandidate> merge(
		List<ProviderCandidate> preferred,
		List<ProviderCandidate> others
	) {
		if (preferred.isEmpty())
			return others;
		if (others.isEmpty())
			return preferred;

		var merged = new ArrayList<ProviderCandidate>(preferred);
		for (var candidate : others) {
			if (!merged.contains(candidate)) {
				merged.add(candidate);
			}
		}
		return merged;
	}

	private record NameAndLocation(String name, String location) {
	}
}
