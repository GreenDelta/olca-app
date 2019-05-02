package org.openlca.app.tools.mapping.model;

import java.util.HashMap;
import java.util.Optional;

import org.openlca.app.db.Database;
import org.openlca.app.tools.mapping.model.FlowMapEntry.SyncState;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Flow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Replacer implements Runnable {

	private final ReplacerConfig conf;
	private final IDatabase db;
	private final Logger log = LoggerFactory.getLogger(getClass());

	public Replacer(ReplacerConfig conf) {
		this.conf = conf;
		this.db = Database.get();
	}

	@Override
	public void run() {
		if (conf == null || (!conf.processes && !conf.methods)) {
			log.info("no configuration; nothing to replace");
			return;
		}

		HashMap<Long, FlowMapEntry> index = new HashMap<>();
		for (FlowMapEntry entry : conf.mapping.entries) {
			if (entry.syncState != SyncState.MATCHED)
				continue;
			Optional<Flow> opt = conf.provider.persist(entry.targetFlow, db);
			if (!opt.isPresent()) {
				entry.syncState = SyncState.UNFOUND_TARGET;
				continue;
			}
			Flow flow = opt.get();
			// TODO: check the flow...
		}
	}
}
