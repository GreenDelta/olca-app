package org.openlca.app.editors.systems;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

import org.openlca.core.database.EntityCache;
import org.openlca.core.matrix.LongPair;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;

class Statistics {

	@SuppressWarnings("unused")
	private int processCount;
	@SuppressWarnings("unused")
	private int linkCount;
	@SuppressWarnings("unused")
	private int techMatrixSize;
	@SuppressWarnings("unused")
	private boolean connectedGraph;
	@SuppressWarnings("unused")
	private ProcessDescriptor refProcess;
	@SuppressWarnings("unused")
	private List<LinkValue> topInDegrees;
	@SuppressWarnings("unused")
	private List<LinkValue> topOutDegrees;

	private Statistics() {
	}

	public static Statistics calculate(ProductSystem system, EntityCache cache) {
		Statistics statistics = new Statistics();
		try {
			statistics.doCalc(system, cache);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(Statistics.class);
			log.error("failed to calculate product system statistics for "
					+ system, e);
		}
		return statistics;
	}

	private void doCalc(ProductSystem system, EntityCache cache) {
		processCount = system.getProcesses().size();
		linkCount = system.getProcessLinks().size();
		refProcess = Descriptors.toDescriptor(system.getReferenceProcess());
		HashSet<LongPair> processProducts = new HashSet<>();
		Multimap<Long, Long> inEdges = HashMultimap.create();
		Multimap<Long, Long> outEdges = HashMultimap.create();
		for (ProcessLink link : system.getProcessLinks()) {
			processProducts.add(LongPair.of(link.getProviderId(),
					link.getFlowId()));
			inEdges.put(link.getRecipientId(), link.getProviderId());
			outEdges.put(link.getProviderId(), link.getRecipientId());
		}
		techMatrixSize = processProducts.size();
		connectedGraph = isConnectedGraph(system, inEdges);
		topInDegrees = calculateMostLinked(inEdges, 5, cache);
		topOutDegrees = calculateMostLinked(outEdges, 5, cache);
	}

	/**
	 * The product system graph is connected if we can visit every process in
	 * the product system traversing the graph starting from the reference
	 * process and following the incoming process links.
	 */
	private static boolean isConnectedGraph(ProductSystem system,
			Multimap<Long, Long> inEdges) {
		if (system.getReferenceProcess() == null)
			return false;
		HashMap<Long, Boolean> visited = new HashMap<>();
		Queue<Long> queue = new ArrayDeque<>();
		queue.add(system.getReferenceProcess().getId());
		while (!queue.isEmpty()) {
			Long recipient = queue.poll();
			visited.put(recipient, Boolean.TRUE);
			for (Long provider : inEdges.get(recipient)) {
				Boolean state = visited.get(provider);
				if (!Objects.equals(state, Boolean.TRUE)
						&& !queue.contains(provider))
					queue.add(provider);
			}
		}
		for (Long processId : system.getProcesses()) {
			Boolean val = visited.get(processId);
			if (!Objects.equals(val, Boolean.TRUE))
				return false;
		}
		return true;
	}

	private static List<LinkValue> calculateMostLinked(
			Multimap<Long, Long> edges,
			int maxSize, EntityCache cache) {
		Long[] keys = new Long[maxSize];
		int[] degrees = new int[maxSize];
		for (Long id : edges.keySet()) {
			int degree = edges.get(id).size();
			if (degree == 0)
				continue;
			Long key = id;
			for (int i = 0; i < maxSize; i++) {
				if (degree <= degrees[i])
					continue;
				if (keys[i] == null) {
					keys[i] = key;
					degrees[i] = degree;
					break;
				} else {
					Long swapKey = keys[i];
					int swapDegree = degrees[i];
					keys[i] = key;
					degrees[i] = degree;
					key = swapKey;
					degree = swapDegree;
				}
			}
		}
		return createLinkValues(keys, degrees, cache);
	}

	private static List<LinkValue> createLinkValues(Long[] keys, int[] degrees,
			EntityCache cache) {
		List<LinkValue> linkValues = new ArrayList<>();
		for (int i = 0; i < keys.length; i++) {
			Long key = keys[i];
			if (key == null)
				break;
			ProcessDescriptor process = cache.get(ProcessDescriptor.class, key);
			LinkValue value = new LinkValue();
			value.process = process;
			value.degree = degrees[i];
			linkValues.add(value);
		}
		return linkValues;
	}

	public String toJson() {
		return new Gson().toJson(this);
	}

	private static class LinkValue {
		@SuppressWarnings("unused")
		int degree;
		@SuppressWarnings("unused")
		ProcessDescriptor process;
	}

}
