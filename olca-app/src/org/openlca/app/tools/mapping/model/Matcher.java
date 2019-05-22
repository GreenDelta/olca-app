package org.openlca.app.tools.mapping.model;

import org.openlca.core.database.IDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Matcher {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final IMapProvider provider;
	private final IDatabase db;

	private Matcher(IMapProvider provider, IDatabase db) {
		this.provider = provider;
		this.db = db;
	}

	public static FlowMap generate(IMapProvider provider, IDatabase db) {
		if (provider == null || db == null)
			return new FlowMap();
		return new Matcher(provider, db).gen();
	}

	private FlowMap gen() {
		log.debug("generate mappings");
		FlowMap map = new FlowMap();
		for (FlowRef ref : provider.getFlowRefs()) {
			// finding the matches (in parallel?)
			FlowMapEntry e = new FlowMapEntry();
			e.sourceFlow = ref;
			e.syncState = SyncState.UNFOUND_TARGET;
			e.factor = 1.0;
			map.entries.add(e);
		}
		return new FlowMap();
	}

}
