package org.openlca.app.tools.mapping.generator;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.math3.util.Pair;
import org.openlca.app.tools.mapping.model.DBProvider;
import org.openlca.app.tools.mapping.model.IProvider;
import org.openlca.app.util.Labels;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.LocationDao;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.io.maps.FlowRef;
import org.openlca.util.CategoryPathBuilder;

class Matcher {

	private final IDatabase db;
	private final Map<String, FlowRef> targetFlows;
	private final WordMatcher words;

	// helper structures for collecting provider information
	private CategoryPathBuilder categories;
	private Map<Long, String> locations;

	Matcher(IProvider targetSystem) {
		db = targetSystem instanceof DBProvider
				? ((DBProvider) targetSystem).db
				: null;
		this.targetFlows = targetSystem.getFlowRefs().stream()
				.filter(f -> f.flow != null && f.flow.refId != null)
				.collect(Collectors.toMap(f -> f.flow.refId, f -> f));
		this.words = new WordMatcher();
	}

	FlowRef find(FlowRef sflow) {
		if (sflow == null || sflow.flow == null
				|| sflow.flow.refId == null)
			return null;

		// test whether there is a direct match based
		// on the reference IDs
		FlowRef tflow = targetFlows.get(sflow.flow.refId);
		if (tflow != null) {
			checkAddProvider(sflow, tflow);
			return tflow;
		}

		// find the best matching flow by computing and
		// comparing matching scores
		tflow = targetFlows.values()
				.parallelStream()
				.map(tf -> new Pair<>(tf, Score.compute(sflow, tf, words)))
				.reduce((pair1, pair2) -> {
					Score score1 = pair1.getValue();
					Score score2 = pair2.getValue();
					return score2.betterThan(score1) ? pair2 : pair1;
				})
				.map(pair -> pair.getFirst())
				.orElse(null);
		if (tflow != null) {
			checkAddProvider(sflow, tflow);
		}
		return tflow;
	}

	private void checkAddProvider(FlowRef sourceFlow, FlowRef targetFlow) {
		if (db == null || targetFlow == null || targetFlow.flow == null)
			return;
		if (targetFlow.flow.flowType == FlowType.ELEMENTARY_FLOW)
			return;
		ProcessDescriptor prov = findProvider(sourceFlow, targetFlow, db);
		if (prov == null)
			return;
		targetFlow.provider = prov;
		if (categories == null) {
			categories = new CategoryPathBuilder(db);
		}
		targetFlow.providerCategory = categories.build(prov.category);
		if (locations == null) {
			locations = new LocationDao(db).getCodes();
		}
		if (prov.location != null) {
			targetFlow.providerLocation = locations.get(prov.location);
		}
	}

	private ProcessDescriptor findProvider(FlowRef sourceFlow,
			FlowRef targetFlow, IDatabase db) {

		Set<Long> processIDs = null;
		long tid = targetFlow.flow.id;
		if (targetFlow.flow.flowType == FlowType.WASTE_FLOW) {
			processIDs = new FlowDao(db).getWhereInput(tid);
		} else {
			processIDs = new FlowDao(db).getWhereOutput(tid);
		}
		if (processIDs == null || processIDs.isEmpty())
			return null;

		List<ProcessDescriptor> candidates = new ProcessDao(db)
				.getDescriptors(processIDs);
		if (candidates.isEmpty())
			return null;
		if (candidates.size() == 1)
			return candidates.get(0);

		ProcessDescriptor cand = null;
		double score = 0.0;
		for (ProcessDescriptor d : candidates) {
			// include possible location codes in the matching
			// location codes are often added to names in Gabi
			// database
			String pname = Labels.getDisplayName(d);
			double s = words.matchAll(sourceFlow.flow.name, pname);
			if (cand == null || s > score) {
				cand = d;
				score = s;
			}
		}
		return cand;
	}
}
