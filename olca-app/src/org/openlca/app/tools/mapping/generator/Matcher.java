package org.openlca.app.tools.mapping.generator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openlca.io.maps.FlowRef;

class Matcher {

	private final Map<String, FlowRef> targetFlows;

	Matcher(List<FlowRef> targetFlows) {
		this.targetFlows = targetFlows.stream()
				.filter(f -> f.flow != null && f.flow.refId != null)
				.collect(Collectors.toMap(f -> f.flow.refId, f -> f));
	}

	FlowRef find(FlowRef sflow) {
		if (sflow == null || sflow.flow == null
				|| sflow.flow.refId == null)
			return null;

		// test whether there is a direct match based
		// on the reference IDs
		FlowRef tflow = targetFlows.get(sflow.flow.refId);
		if (tflow != null)
			return tflow;

		FlowRef candidate = null;
		double score = 0.0;
		for (FlowRef c : targetFlows.values()) {
			double s = score(sflow, c);
			if (s > score) {
				candidate = c;
				score = s;
			}
		}
		return candidate;
	}

	private double score(FlowRef sflow, FlowRef tflow) {
		if (sflow.flow == null || tflow.flow == null)
			return 0;
		double nameScore = Words.match(sflow.flow.name, tflow.flow.name);
		if (nameScore == 0)
			return 0;

		double catScore = Words.match(sflow.flowCategory, tflow.flowCategory);
		double locScore = Words.match(sflow.flowLocation, tflow.flowLocation);

		double score = nameScore + (0.25 * catScore) + (0.25 * locScore);
		if (sflow.flow.flowType == tflow.flow.flowType) {
			score *= 1.1;
		}
		// TODO flow property, unit, location in names etc.
		return score;
	}

}
