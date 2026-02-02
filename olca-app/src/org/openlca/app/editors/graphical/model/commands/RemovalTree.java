package org.openlca.app.editors.graphical.model.commands;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openlca.core.model.ProcessLink;
import org.openlca.util.Lists;

/// Calculates the removal tree for a path in a product system.
///
/// Starting from a root process `q`:
/// - Collect all providers `P` recursively reachable from `q`
/// - Remove any provider `p` that has a consumer `o`
///   where `o != q` and `o` is not in `P`
/// - Repeat until no such provider is found
/// - The remaining providers and their links can be safely removed
///
/// Note that the root process `q` will always be included and, thus, all of its
/// links will be also added to the removal tree.
public record RemovalTree(Set<Long> providers, List<ProcessLink> links) {

	public static RemovalTree of(List<ProcessLink> links, long root) {
		return Lists.isEmpty(links)
			? new RemovalTree(Set.of(root), List.of())
			: new Builder(links, root).build();
	}

	public static RemovalTree of(List<ProcessLink> links, ProcessLink rootLink) {
		if (rootLink == null) {
			return new RemovalTree(Set.of(), List.of());
		}
		return Lists.isEmpty(links)
			? new RemovalTree(Set.of(rootLink.providerId), List.of(rootLink))
			: new Builder(links, rootLink.providerId).build();
	}

	private static class Builder {

		private final List<ProcessLink> links;
		private final long root;

		/// map process -> set of providers it uses
		private final Map<Long, Set<Long>> procProvs = new HashMap<>();

		/// map provider -> set of processes that use it
		private final Map<Long, Set<Long>> provProcs = new HashMap<>();

		Builder(List<ProcessLink> links, long root) {
			this.links = links;
			this.root = root;

			// index the provider links
			for (var link : links) {
				procProvs
					.computeIfAbsent(link.processId, k -> new HashSet<>())
					.add(link.providerId);
				provProcs
					.computeIfAbsent(link.providerId, k -> new HashSet<>())
					.add(link.processId);
			}
		}

		RemovalTree build() {
			var providers = collectProviders();
			var removals = reduceProviders(providers);
			removals.add(root);

			var remLinks = new ArrayList<ProcessLink>();
			for (var link : links) {
				if (removals.contains(link.processId)
					|| removals.contains(link.providerId)) {
					remLinks.add(link);
				}
			}
			return new RemovalTree(removals, remLinks);
		}

		/// Collect all providers recursively reachable from the root process.
		private Set<Long> collectProviders() {

			var seen = new HashSet<Long>();
			var stack = new ArrayDeque<Long>();
			stack.push(root);

			while (!stack.isEmpty()) {
				var provider = stack.pop();
				var next = procProvs.get(provider);
				if (next == null)
					continue;
				for (var p : next) {
					if (seen.add(p)) {
						stack.push(p);
					}
				}
			}
			return seen;
		}

		/// Reduces the set of providers to only those that can be safely removed.
		/// A provider can be removed if all its consumers are either the root or
		/// already in the removal set.
		private Set<Long> reduceProviders(Set<Long> providers) {

			// in the first iteration, identify all providers that
			// are used outside the provider tree
			var queue = new ArrayDeque<Long>();
			var outDegree = new HashMap<Long, Integer>();
			for (var p : providers) {
				var consumers = provProcs.get(p);
				if (consumers == null) continue;
				int extCount = 0;
				for (var c : consumers) {
					if (c != root && !providers.contains(c)) {
						extCount++;
					}
				}
				if (extCount > 0) {
					outDegree.put(p, extCount);
					queue.add(p);
				}
			}

			// we cannot remove providers with links outside the
			// tree, and recursively also not their providers
			// within the tree and so on
			var bad = new HashSet<Long>();
			while (!queue.isEmpty()) {
				var p = queue.poll();
				if (!bad.add(p)) continue;

				// providers of the bad provider
				var provs = procProvs.get(p);
				if (provs == null) continue;
				for (var prov : provs) {
					if (!providers.contains(prov)) continue;
					int outDeg = outDegree.getOrDefault(prov, 0) + 1;
					outDegree.put(prov, outDeg);
					if (outDeg == 1) {
						// 1 means we found a new provider of a provider
						// that is used outside the tree
						queue.add(prov);
					}
				}
			}

			if (bad.isEmpty()) return providers;
			var result = new HashSet<>(providers);
			result.removeAll(bad);
			return result;
		}
	}
}
