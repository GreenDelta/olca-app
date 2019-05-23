package org.openlca.app.tools.mapping.model;

import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Flow;

public class FlowMaps {

	private final IProvider provider;
	private final IDatabase db;

	private FlowMaps(IProvider provider, IDatabase db) {
		this.provider = provider;
		this.db = db;
	}

	public static void sync(FlowMap map, IProvider provider, IDatabase db) {
		if (map == null || provider == null || db == null)
			return;
		FlowMaps maps = new FlowMaps(provider, db);
		maps.sync(map);
	}

	private void sync(FlowMap map) {
		if (map == null)
			return;
		FlowDao dao = new FlowDao(db);
		Map<String, Flow> dbFlows = dao.getAll()
				.stream().collect(Collectors
						.toMap(d -> d.refId, d -> d));
		HashSet<String> handled = new HashSet<>();

		for (FlowMapEntry e : map.entries) {

			// get the source flow
			if (e.sourceFlow == null || e.sourceFlow.flow == null) {
				e.status = Status.error("no source flow defined");
				continue;
			}
			String sourceID = e.sourceFlow.flow.refId;
			if (sourceID == null) {
				e.status = Status.error("source flow has no UUID");
				continue;
			}
			if (handled.contains(sourceID)) {
				e.status = Status.error("duplicate mapping");
				continue;
			}
			Flow sourceFlow = dbFlows.get(sourceID);
			if (sourceFlow == null) {
				e.status = Status.error("source flow not found in database");
				continue;
			}
			// TODO: validate a possible flow property and unit

			if (e.targetFlow == null || e.targetFlow.flow == null) {
				e.status = Status.error("not target flow defined");
				continue;
			}
			String targetID = e.targetFlow.flow.refId;
			if (targetID == null) {
				e.status = Status.error("target flow has no UUID");
				continue;
			}
			Flow targetFlow = dbFlows.get(targetID);
			if (targetFlow != null) {
				// TODO: validate flow property + unit
			} else {
				// TODO: check this against the flow references that can
				// be retrieved from the provider.
			}

			e.status = Status.ok();
		}
	}
}
