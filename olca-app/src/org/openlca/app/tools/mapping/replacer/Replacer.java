package org.openlca.app.tools.mapping.replacer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.tools.mapping.model.DBProvider;
import org.openlca.app.util.Labels;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.database.ProductSystemDao;
import org.openlca.core.io.maps.FlowMapEntry;
import org.openlca.core.io.maps.FlowRef;
import org.openlca.core.io.maps.MappingStatus;
import org.openlca.core.model.Flow;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.util.FlowReplacer;
import org.openlca.util.VersionUpdate;
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

	FactorProvider factors;

	/// Contains the IDs of processes where flows should be replaced.
	Set<Long> processes = new HashSet<>();
	/// Contains the IDs of LCIA categories where flows should be replaced.
	Set<Long> impacts = new HashSet<>();

	public Replacer(ReplacerConfig conf) {
		this.conf = conf;
		this.db = Database.get();
	}

	@Override
	public void run() {
		if (conf == null || (conf.models.isEmpty())) {
			log.info("no configuration; nothing to replace");
			return;
		}

		// collect the IDs of processes and LCIA categories
		// where flows should be replaced
		processes.clear();
		impacts.clear();
		for (var model : conf.models) {
			if (model.isFromLibrary())
				continue;
			if (model.type == ModelType.PROCESS) {
				processes.add(model.id);
			} else if (model.type == ModelType.IMPACT_METHOD) {
				ImpactMethodDao dao = new ImpactMethodDao(db);
				dao.getCategoryDescriptors(model.id)
						.forEach(d -> impacts.add(d.id));
			}
		}

		buildIndices();
		if (entries.isEmpty()) {
			log.info("found no flows that can be mapped");
			return;
		}
		log.info("found {} flows that can be mapped", entries.size());

		try {

			// start and wait for the cursors to finish
			log.info("start updatable cursors");
			var cursors = createCursors();
			var pool = Executors.newFixedThreadPool(4);
			for (var c : cursors) {
				pool.execute(c);
			}
			pool.shutdown();
			int i = 0;
			while (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
				i++;
				log.info("waiting for cursors to finish; {} seconds", i * 10);
			}
			log.info("cursors finished");
			db.getEntityFactory().getCache().evictAll();

			// TODO when products were replaced we also need to check
			// whether these products are used in the quant. ref. of
			// product systems and project variants and convert the
			// amounts there.

			updateVersions(cursors);

			// collect and log statistics
			var stats = new Stats();
			for (var c : cursors) {
				stats.add(c.stats);
				c.stats.log(c.getClass().getName(), flows);
			}

			boolean deleteMapped = false;
			Set<Long> usedFlows = null;
			if (conf.deleteMapped) {
				if (stats.failures > 0) {
					log.warn("Will not delete mapped flows because"
									+ " there were {} failures in replacement process",
							stats.failures);
				} else {
					deleteMapped = true;
					usedFlows = FlowReplacer.getUsedFlowsOf(db).stream()
							.map(f -> f.id)
							.collect(Collectors.toSet());
				}
			}

			// update the mapping entries
			for (Long flowID : entries.keySet()) {
				FlowMapEntry e = entries.get(flowID);
				if (flowID == null || e == null)
					continue;
				if (stats.hadFailures(flowID)) {
					e.sourceFlow().status = MappingStatus.error(M.ReplacementError);
					continue;
				}
				if (deleteMapped && !usedFlows.contains(flowID)) {
					FlowDao dao = new FlowDao(db);
					Flow flow = dao.getForId(flowID);
					if (!flow.isFromLibrary()) {
						dao.delete(flow);
						log.info("removed mapped flow {} uuid={}",
								Labels.name(flow), flow.refId);
						e.sourceFlow().status = MappingStatus.ok(M.AppliedAndRemoved);
						continue;
					}
				}
				e.sourceFlow().status = MappingStatus.ok(M.AppliedNotRemoved);
			}
		} catch (Exception e) {
			log.error("Flow replacement failed", e);
		}
	}

	private List<UpdatableCursor> createCursors() {
		var cursors = new ArrayList<UpdatableCursor>();
		if (!processes.isEmpty()) {
			cursors.add(new AmountCursor(ModelType.PROCESS, this));
			cursors.add(new ProcessLinkCursor(this));
			cursors.add(new AllocationCursor(this));
		}
		if (!impacts.isEmpty()) {
			cursors.add(new AmountCursor(ModelType.IMPACT_CATEGORY, this));
		}
		return cursors;
	}

	private void buildIndices() {

		// first persist all target flows in the database that
		// do not have an error flag
		List<FlowRef> targetFlows = conf.mapping.entries.stream()
				.filter(e -> e.targetFlow() != null
						&& e.targetFlow().status != null
						&& !e.targetFlow().status.isError())
				.map(FlowMapEntry::targetFlow)
				.collect(Collectors.toList());

		conf.provider.persist(targetFlows, db);

		DBProvider dbProvider = new DBProvider(db);
		for (FlowMapEntry e : conf.mapping.entries) {

			// only do the replacement for matched mapping entries
			// (both flows should have no error flag)
			if (e.sourceFlow() == null
					|| e.sourceFlow().status == null
					|| e.sourceFlow().status.isError()
					|| e.targetFlow() == null
					|| e.targetFlow().status == null
					|| e.targetFlow().status.isError())
				continue;

			// sync the flows
			Flow source = dbProvider.sync(e.sourceFlow());
			if (source == null)
				continue;
			Flow target = dbProvider.sync(e.targetFlow());
			if (target == null)
				continue;

			entries.put(source.id, e);
			flows.put(source.id, source);
			flows.put(target.id, target);
		}
		factors = new FactorProvider(db, flows);
	}

	private void updateVersions(List<UpdatableCursor> cursors) {

		// when processes changed, product system may change too
		var systems = processes.isEmpty() ? Set.of() :
				new ProductSystemDao(db)
						.getDescriptors()
						.stream()
						.map(s -> s.id)
						.collect(Collectors.toSet());

		var updatedImpacts = new HashSet<Long>();
		var updatedProcesses = new HashSet<Long>();
		var updatedSystems = new HashSet<Long>();
		for (var c : cursors) {
			for (var id : c.updatedModels) {
				if (impacts.contains(id)) {
					updatedImpacts.add(id);
				} else if (processes.contains(id)) {
					updatedProcesses.add(id);
				} else if (systems.contains(id)) {
					updatedSystems.add(id);
				}
			}
		}

		if (!updatedImpacts.isEmpty()) {
			VersionUpdate.of(db, ImpactCategory.class).run(updatedImpacts);
		}
		if (!updatedProcesses.isEmpty()) {
			VersionUpdate.of(db, Process.class).run(updatedProcesses);
		}
		if (!updatedSystems.isEmpty()) {
			VersionUpdate.of(db, ProductSystem.class).run(updatedSystems);
		}
	}
}
