package org.openlca.app.tools.mapping.replacer;

import java.util.HashMap;
import java.util.HashSet;

import org.openlca.app.util.Labels;
import org.openlca.core.model.Flow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Stats {

	static final byte REPLACEMENT = 0;
	static final byte FAILURE = 1;

	int failures;
	int replacements;
	final HashMap<Long, Integer> flowFailures = new HashMap<>();
	final HashMap<Long, Integer> flowReplacements = new HashMap<>();

	void add(Stats s) {
		if (s == null)
			return;
		failures += s.failures;
		replacements += s.replacements;
		for (Long flowID : s.flowFailures.keySet()) {
			int c = s.flowFailures.getOrDefault(flowID, 0);
			flowFailures.put(flowID,
					c + flowFailures.getOrDefault(flowID, 0));
		}
		for (Long flowID : s.flowReplacements.keySet()) {
			int c = s.flowReplacements.getOrDefault(flowID, 0);
			flowReplacements.put(flowID,
					c + flowReplacements.getOrDefault(flowID, 0));
		}
	}

	boolean hadFailures(long flowID) {
		Integer failures = flowFailures.get(flowID);
		if (failures == null)
			return false;
		return failures > 0;
	}

	void inc(long flowID, byte type) {
		HashMap<Long, Integer> flowStats;
		if (type == REPLACEMENT) {
			replacements++;
			flowStats = flowReplacements;
		} else {
			failures++;
			flowStats = flowFailures;
		}
		Integer count = flowStats.get(flowID);
		if (count == null) {
			flowStats.put(flowID, 1);
		} else {
			flowStats.put(flowID, count + 1);
		}
	}

	void log(String context, HashMap<Long, Flow> flows) {
		Logger log = LoggerFactory.getLogger(getClass());
		if (replacements == 0 && failures == 0) {
			log.info("No flows replaced in {}", context);
			return;
		}
		if (failures > 0) {
			log.warn("There were failures while replacing flows in {}", context);
		}
		log.info("{} replacements and {} failures in {}",
				replacements, failures, context);
		if (!log.isTraceEnabled())
			return;
		HashSet<Long> ids = new HashSet<>();
		ids.addAll(flowFailures.keySet());
		ids.addAll(flowReplacements.keySet());
		for (Long id : ids) {
			Flow flow = flows.get(id);
			if (flow == null)
				continue;
			int rcount = flowReplacements.getOrDefault(id, 0);
			int fcount = flowFailures.getOrDefault(id, 0);
			if (rcount == 0 && fcount == 0)
				continue;
			log.trace("Flow {} uuid={} :: {} replacements, {} failures in {}",
					Labels.name(flow), flow.refId, rcount, fcount, context);
		}
	}
}