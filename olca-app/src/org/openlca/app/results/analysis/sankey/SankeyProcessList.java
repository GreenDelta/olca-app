package org.openlca.app.results.analysis.sankey;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import org.openlca.core.matrix.ProcessLinkSearchMap;
import org.openlca.core.model.ProcessLink;

/**
 * Calculates the processes that are visible in the Sankey diagram.
 */
class SankeyProcessList {

	private SankeyResult sankeyResult;
	private long refProcess;
	private double cutoff;
	private ProcessLinkSearchMap linkSearchMap;

	private SankeyProcessList(SankeyResult result, long refProcess,
			double cutoff, ProcessLinkSearchMap linkSearchMap) {
		this.sankeyResult = result;
		this.refProcess = refProcess;
		this.cutoff = cutoff;
		this.linkSearchMap = linkSearchMap;
	}

	public static Set<Long> calculate(SankeyResult result, long refProcess,
			double cutoff, ProcessLinkSearchMap linkSearchMap) {
		return new SankeyProcessList(result, refProcess, cutoff, linkSearchMap)
				.calculate();
	}

	private Set<Long> calculate() {
		List<Long> initial = sankeyResult.getProcesseIdsAboveCutoff(cutoff);
		Set<Long> processesToDraw = new HashSet<Long>(initial);
		processesToDraw.add(refProcess);
		fillUp(processesToDraw);
		return processesToDraw;
	}

	/**
	 * Checks if each process has a path to the reference process, if not it
	 * searches a way to the reference or another connected node and adds the
	 * missing nodes.
	 */
	private void fillUp(Set<Long> processIds) {

		Set<Long> unconnected = new HashSet<>(processIds);
		Set<Long> connected = new HashSet<>();
		Queue<Long> queue = new ArrayDeque<>();
		queue.add(refProcess);
		while (!queue.isEmpty()) {
			Long recipient = queue.poll();
			unconnected.remove(recipient);
			connected.add(recipient);
			for (ProcessLink link : linkSearchMap.getIncomingLinks(recipient)) {
				Long provider = link.getProviderId();
				if (!processIds.contains(provider))
					continue;
				if (!queue.contains(provider) && !connected.contains(provider))
					queue.add(provider);
			}
		}
		for (Long processId : unconnected) {
			Stack<Long> path = searchPathFor(processId, connected);
			for (Long id : path) {
				processIds.add(id);
				connected.add(id);
			}
		}
	}

	/**
	 * Find a way from the given process to the connected graph following the
	 * path with biggest weight and return the list of new processes that
	 */
	private Stack<Long> searchPathFor(long processToConnect,
			Set<Long> connectedGraph) {

		Stack<Long> path = new Stack<>();
		path.push(processToConnect);
		HashSet<Long> visited = new HashSet<>();
		visited.add(processToConnect);

		Stack<List<Long>> candidateStack = new Stack<>();
		candidateStack.push(getWeightedRecipients(processToConnect));

		while (!candidateStack.isEmpty()) {

			List<Long> candidates = candidateStack.peek();
			if (candidates.isEmpty()) {
				candidateStack.pop();
				Long v = path.pop();
				visited.add(v);
				continue;
			}

			for (Long candidate : candidates) {
				if (connectedGraph.contains(candidate))
					return path; // found a way to the connected graph
			}

			Long next = null;
			for (int i = 0; i < candidates.size(); i++) {
				Long candidate = candidates.remove(0); // take the first = 0
				if (!path.contains(candidate)) {
					next = candidate;
					break;
				}
			}

			if (next != null) {
				path.push(next);
				List<Long> nextCandidates = new ArrayList<>();
				for (Long nextCandidate : getWeightedRecipients(next)) {
					if (!visited.contains(nextCandidate)
							&& !path.contains(nextCandidate)) {
						nextCandidates.add(nextCandidate);
					}
				}
				candidateStack.push(nextCandidates);
			}
		}
		return path;
	}

	private List<Long> getWeightedRecipients(long processId) {
		List<WeightedProcess> recipients = new ArrayList<>();
		for (ProcessLink link : linkSearchMap.getOutgoingLinks(processId)) {
			WeightedProcess wp = new WeightedProcess();
			wp.id = link.getRecipientId();
			wp.weight = Math.abs(sankeyResult.getLinkContribution(link));
			recipients.add(wp);
		}
		Collections.sort(recipients);
		List<Long> ids = new ArrayList<>();
		for (WeightedProcess recipient : recipients)
			ids.add(recipient.id);
		return ids;
	}

	private class WeightedProcess implements Comparable<WeightedProcess> {

		private long id;
		private double weight;

		@Override
		public int compareTo(final WeightedProcess o) {
			return -Double.compare(weight, o.weight);
		}
	}

}
