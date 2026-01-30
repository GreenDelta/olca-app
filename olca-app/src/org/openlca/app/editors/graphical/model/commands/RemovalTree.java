package org.openlca.app.editors.graphical.model.commands;

import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;

import java.util.*;

/**
 * Calculates the removal tree for a path in a product system.
 * <p>
 * Starting from a root process q:
 * <ul>
 *   <li>Collect all providers P recursively reachable from q</li>
 *   <li>Remove any provider p that has a consumer o where o != q and o is not in P</li>
 *   <li>Repeat until no such provider is found</li>
 *   <li>The remaining providers and their links can be safely removed</li>
 * </ul>
 */
public class RemovalTree {

	private final Set<Long> removableProcesses;
	private final List<ProcessLink> removableLinks;

	private RemovalTree(Set<Long> removableProcesses, List<ProcessLink> removableLinks) {
		this.removableProcesses = removableProcesses;
		this.removableLinks = removableLinks;
	}

	public Set<Long> getRemovableProcesses() {
		return removableProcesses;
	}

	public List<ProcessLink> getRemovableLinks() {
		return removableLinks;
	}

	/**
	 * Calculates the removal tree for the given product system starting from the root process.
	 *
	 * @param system the product system
	 * @param rootId the ID of the root process to start removal from
	 * @return the removal tree containing removable processes and links
	 */
	public static RemovalTree of(ProductSystem system, long rootId) {
		var links = system.processLinks;

		// Index: process -> set of providers it uses
		var processToProviders = new HashMap<Long, Set<Long>>();
		// Index: provider -> set of processes that use it
		var providerToProcesses = new HashMap<Long, Set<Long>>();

		for (var link : links) {
			processToProviders
					.computeIfAbsent(link.processId, k -> new HashSet<>())
					.add(link.providerId);
			providerToProcesses
					.computeIfAbsent(link.providerId, k -> new HashSet<>())
					.add(link.processId);
		}

		// Collect all providers recursively reachable from root
		var providers = collectProviders(rootId, processToProviders);

		// Reduce to only those that can be safely removed
		var removable = reduceProviders(rootId, providers, providerToProcesses);

		// Collect links where either process or provider is in the removable set
		var removableLinks = new ArrayList<ProcessLink>();
		for (var link : links) {
			if (removable.contains(link.processId) || removable.contains(link.providerId)) {
				removableLinks.add(link);
			}
		}

		return new RemovalTree(removable, removableLinks);
	}

	/**
	 * Collects all providers recursively reachable from the given process.
	 */
	private static Set<Long> collectProviders(long processId, Map<Long, Set<Long>> processToProviders) {
		var seen = new HashSet<Long>();
		var stack = new ArrayDeque<Long>();

		var initial = processToProviders.get(processId);
		if (initial != null) {
			stack.addAll(initial);
			seen.addAll(initial);
		}

		while (!stack.isEmpty()) {
			var provider = stack.pop();
			var nextProviders = processToProviders.get(provider);
			if (nextProviders == null)
				continue;
			for (var next : nextProviders) {
				if (seen.add(next)) {
					stack.push(next);
				}
			}
		}

		return seen;
	}

	/**
	 * Reduces the set of providers to only those that can be safely removed.
	 * A provider can be removed if all its consumers are either the root or already in the removal set.
	 */
	private static Set<Long> reduceProviders(
			long rootId,
			Set<Long> providers,
			Map<Long, Set<Long>> providerToProcesses
	) {
		var current = new HashSet<>(providers);

		while (true) {
			var toRemove = new ArrayList<Long>();

			for (var provider : current) {
				var consumers = providerToProcesses.get(provider);
				if (consumers == null)
					continue;

				// Check if provider has an external consumer
				boolean hasExternalConsumer = false;
				for (var consumer : consumers) {
					if (consumer != rootId && !current.contains(consumer)) {
						hasExternalConsumer = true;
						break;
					}
				}

				if (hasExternalConsumer) {
					toRemove.add(provider);
				}
			}

			if (toRemove.isEmpty()) {
				break;
			}

			current.removeAll(toRemove);
		}

		return current;
	}
}
