package org.openlca.app.tools.mapping.replacer;

import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.openlca.app.db.Database;
import org.openlca.app.tools.mapping.model.FlowMapEntry;
import org.openlca.app.tools.mapping.model.SyncState;
import org.openlca.app.util.Labels;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.matrix.cache.ConversionTable;
import org.openlca.core.model.Flow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Replacer implements Runnable {

	private final ReplacerConfig conf;
	private final Logger log = LoggerFactory.getLogger(getClass());

	final IDatabase db;
	// the valid entries that could be applied: source flow ID -> mapping.
	final HashMap<Long, FlowMapEntry> entries = new HashMap<>();
	// the source and target flows in the database: flow ID -> flow.
	final HashMap<Long, Flow> flows = new HashMap<>();
	ConversionTable conversions;

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

		buildIndices();
		if (entries.isEmpty()) {
			log.info("found no flows that can be mapped");
			return;
		}
		log.info("found {} flows that can be mapped", entries.size());

		try {

			log.info("start updatable cursors");
			Cursor exchangeCursor = null;
			Cursor impactCursor = null;
			ExecutorService pool = Executors.newFixedThreadPool(4);
			if (conf.processes) {
				exchangeCursor = new Cursor(Cursor.EXCHANGES, this);
				pool.execute(exchangeCursor);
			}
			if (conf.methods) {
				impactCursor = new Cursor(Cursor.IMPACTS, this);
				pool.execute(impactCursor);
			}

			// waiting for the cursors to finish
			pool.shutdown();
			int i = 0;
			while (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
				i++;
				log.info("waiting for cursors to finish; {} seconds", i * 10);
			}
			log.info("cursors finished");

			// TODO when products were replaced we also need to check
			// whether these products are used in the quant. ref. of
			// product systems and project variants and convert the
			// amounts there.
			// TODO also: we need to replace wuch flows in allocation
			// factors; the application of the conversion factor is
			// not required there.

			// collect and log statistics
			Stats stats = new Stats();
			if (exchangeCursor != null) {
				stats.add(exchangeCursor.stats);
				exchangeCursor.stats.log("exchanges", flows);
			}
			if (impactCursor != null) {
				stats.add(impactCursor.stats);
				impactCursor.stats.log("impacts", flows);
			}

			boolean deleteMapped = false;
			Set<Long> usedFlows = null;
			if (conf.deleteMapped && conf.processes && conf.methods) {
				if (stats.failures > 0) {
					log.warn("Will not delete mapped flows because"
							+ " there were {} failures in replacement process",
							stats.failures);
				} else {
					deleteMapped = true;
					usedFlows = new FlowDao(db).getUsed();
				}
			}

			// update the mapping entries
			for (Long flowID : entries.keySet()) {
				FlowMapEntry e = entries.get(flowID);
				if (flowID == null || e == null)
					continue;
				if (stats.hadFailures(flowID)) {
					e.syncState = SyncState.INVALID_SOURCE;
					e.syncMessage = "Replacement error";
					continue;
				}
				if (deleteMapped && !usedFlows.contains(flowID)) {
					FlowDao dao = new FlowDao(db);
					Flow flow = dao.getForId(flowID);
					dao.delete(flow);
					log.info("removed mapped flow {} uuid={}",
							Labels.getDisplayName(flow), flow.refId);
					e.syncMessage = "Applied and removed";
				} else {
					e.syncMessage = "Applied (not removed)";
				}
				e.syncState = SyncState.APPLIED;
			}

		} catch (Exception e) {
			log.error("Flow replacement failed", e);
		}
	}

	private void buildIndices() {
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

			entries.put(source.id, entry);
			flows.put(source.id, source);
			flows.put(target.id, target);
		}
		conversions = ConversionTable.create(db);
	}
}
