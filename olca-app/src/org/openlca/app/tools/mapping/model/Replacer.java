package org.openlca.app.tools.mapping.model;

import java.util.HashMap;
import java.util.Optional;

import org.openlca.app.db.Database;
import org.openlca.app.tools.mapping.model.FlowMapEntry.SyncState;
import org.openlca.core.database.FlowDao;
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

		HashMap<Long, FlowMapEntry> index = buildIndex();
		if (index.isEmpty()) {
			log.info("found no flows that can be mapped");
			return;
		}
		log.info("found {} flows that can be mapped", index.size());

	}

	private HashMap<Long, FlowMapEntry> buildIndex() {
		HashMap<Long, FlowMapEntry> index = new HashMap<>();
		FlowDao dao = new FlowDao(db);
		for (FlowMapEntry entry : conf.mapping.entries) {

			// only do the replacement for matched mapping entries
			if (entry.syncState != SyncState.MATCHED)
				continue;

			// sync the source flow
			Flow source = dao.getForRefId(entry.sourceFlow.flow.refId);
			if (source == null) {
				entry.syncState = SyncState.UNFOUND_SOURCE;
				continue;
			}
			if (!entry.sourceFlow.syncWith(source)) {
				entry.syncState = SyncState.INVALID_SOURCE;
				continue;
			}

			// sync the target flow
			Optional<Flow> tOpt = conf.provider.persist(entry.targetFlow, db);
			if (!tOpt.isPresent()) {
				entry.syncState = SyncState.UNFOUND_TARGET;
				continue;
			}
			Flow target = tOpt.get();
			if (!entry.targetFlow.syncWith(target)) {
				entry.syncState = SyncState.INVALID_TARGET;
				continue;
			}

			index.put(source.id, entry);
		}
		return index;
	}
}
