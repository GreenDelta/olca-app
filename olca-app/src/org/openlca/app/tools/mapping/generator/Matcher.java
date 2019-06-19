package org.openlca.app.tools.mapping.generator;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.openlca.util.Strings;

class Matcher {

	private final IDatabase db;
	private final Map<String, FlowRef> targetFlows;

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

		FlowRef candidate = null;
		double score = 0.0;
		for (FlowRef c : targetFlows.values()) {
			double s = score(sflow, c);
			if (s > score) {
				candidate = c;
				score = s;
			}
		}
		if (candidate != null) {
			checkAddProvider(sflow, candidate);
		}
		return candidate;
	}

	private double score(FlowRef sflow, FlowRef tflow) {
		if (sflow.flow == null || tflow.flow == null)
			return 0;
		double nameScore = Words.match(sflow.flow.name, tflow.flow.name);
		if (nameScore < 0.01)
			return 0;

		double catScore = Words.match(sflow.flowCategory, tflow.flowCategory);
		double locScore = Words.match(sflow.flowLocation, tflow.flowLocation);
		double score = nameScore + (0.25 * catScore) + (0.25 * locScore);

		// score flows with same flow type and reference units a bit higher
		if (sflow.flow.flowType == tflow.flow.flowType) {
			score *= 1.1;
		}
		if (sflow.unit != null && tflow.unit != null
				&& Strings.nullOrEqual(sflow.unit.name, tflow.unit.name)) {
			score *= 1.1;
		}
		return score;
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
			double s = Words.match(sourceFlow.flow.name, pname);
			if (cand == null || s > score) {
				cand = d;
				score = s;
			}
		}
		return cand;
	}
}
