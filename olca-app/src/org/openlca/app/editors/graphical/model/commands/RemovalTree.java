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
/// Note that the root process `q` will be always and thus, all of its
/// links will be also added to the removal tree.
public record RemovalTree(Set<Long> providers, List<ProcessLink> links) {

	public static RemovalTree of(List<ProcessLink> links, long root) {
		if (Lists.isEmpty(links)) {
			return new RemovalTree(Set.of(root), List.of());
		}
		return new Builder(links, root).build();
	}

	public static RemovalTree of(List<ProcessLink> links, ProcessLink rootLink) {
		if (rootLink == null) {
			return new RemovalTree(Set.of(), List.of());
		}
		if (Lists.isEmpty(links)) {
			return new RemovalTree(Set.of(rootLink.providerId), List.of(rootLink));
		}
		var tree = new Builder(links, rootLink.providerId).build();
		tree.links.add(rootLink);
		return tree;
	}

	private static class Builder {

		private final List<ProcessLink> links;
		private final long root;

		/// map process -> set of providers it uses
		private final Map<Long, Set<Long>> procProvs = new HashMap<>();

		/// map provider -> set of processes that use it
		private final  Map<Long, Set<Long>> provProcs = new HashMap<>();

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
			var candidates = new HashSet<>(providers);
			while (true) {
				var withExtLinks = new HashSet<Long>();
				for (var candidate : candidates) {
					var consumers = provProcs.get(candidate);
					if (consumers == null)
						continue;
					for (var c : consumers) {
						if (c != root && !candidates.contains(c)) {
							withExtLinks.add(candidate);
							break;
						}
					}
				}
				if (withExtLinks.isEmpty()) break;
				candidates.removeAll(withExtLinks);
			}
			return candidates;
		}
	}
}
