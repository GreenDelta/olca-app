package org.openlca.app.editors.systems;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

import org.openlca.app.db.Database;
import org.openlca.app.util.ErrorReporter;
import org.openlca.core.database.EntityCache;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.NativeSql;
import org.openlca.core.matrix.cache.ProcessTable;
import org.openlca.core.matrix.index.LongPair;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.model.descriptors.ProductSystemDescriptor;
import org.openlca.core.model.descriptors.ResultDescriptor;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import gnu.trove.map.hash.TLongLongHashMap;

class Statistics {

	private final ProductSystem system;
	private final EntityCache cache;

	int processCount;
	int linkCount;
	int techMatrixSize;
	boolean connectedGraph;
	ProcessDescriptor refProcess;
	List<LinkDegree> topInDegrees;
	List<LinkDegree> topOutDegrees;

	int singleProviderLinkCount;
	int defaultProviderLinkCount;
	int multiProviderLinkCount;

	private Statistics(ProductSystem system, EntityCache cache) {
		this.system = system;
		this.cache = cache;
	}

	public static Statistics calculate(ProductSystem system, EntityCache cache) {
		var statistics = new Statistics(system, cache);
		try {
			statistics.calculate();
		} catch (Exception e) {
			ErrorReporter.on(
				"Failed to calculate product system statistics for " + system, e);
		}
		return statistics;
	}

	private void calculate() {
		processCount = system.processes.size();
		linkCount = system.processLinks.size();
		refProcess = Descriptor.of(system.referenceProcess);
		HashSet<LongPair> processProducts = new HashSet<>();
		Multimap<Long, Long> inEdges = HashMultimap.create();
		Multimap<Long, Long> outEdges = HashMultimap.create();
		for (var link : system.processLinks) {
			processProducts.add(LongPair.of(link.providerId, link.flowId));
			inEdges.put(link.processId, link.providerId);
			outEdges.put(link.providerId, link.processId);
		}
		techMatrixSize = processProducts.size();
		connectedGraph = isConnectedGraph(inEdges);
		topInDegrees = calculateMostLinked(inEdges, 5);
		topOutDegrees = calculateMostLinked(outEdges, 5);
		collectProviderInfos(system, Database.get());
	}

	/**
	 * The product system graph is connected if we can visit every process in the
	 * product system traversing the graph starting from the reference process and
	 * following the incoming process links.
	 */
	private boolean isConnectedGraph(Multimap<Long, Long> inEdges) {
		if (system.referenceProcess == null)
			return false;
		HashMap<Long, Boolean> visited = new HashMap<>();
		Queue<Long> queue = new ArrayDeque<>();
		queue.add(system.referenceProcess.id);
		while (!queue.isEmpty()) {
			Long recipient = queue.poll();
			visited.put(recipient, Boolean.TRUE);
			for (Long provider : inEdges.get(recipient)) {
				Boolean state = visited.get(provider);
				if (!Objects.equals(state, Boolean.TRUE)
					&& !queue.contains(provider))
					queue.add(provider);
			}
		}
		for (Long processId : system.processes) {
			Boolean val = visited.get(processId);
			if (!Objects.equals(val, Boolean.TRUE))
				return false;
		}
		return true;
	}

	private List<LinkDegree> calculateMostLinked(
		Multimap<Long, Long> edges, int maxSize) {
		Long[] keys = new Long[maxSize];
		int[] degrees = new int[maxSize];
		for (Long id : edges.keySet()) {
			int degree = edges.get(id).size();
			if (degree == 0)
				continue;
			Long key = id;
			for (int i = 0; i < maxSize; i++) {
				if (degree <= degrees[i])
					continue;
				if (keys[i] == null) {
					keys[i] = key;
					degrees[i] = degree;
					break;
				} else {
					Long swapKey = keys[i];
					int swapDegree = degrees[i];
					keys[i] = key;
					degrees[i] = degree;
					key = swapKey;
					degree = swapDegree;
				}
			}
		}
		return createLinkValues(keys, degrees);
	}

	private List<LinkDegree> createLinkValues(Long[] ids, int[] degrees) {
		var linkValues = new ArrayList<LinkDegree>();
		for (int i = 0; i < ids.length; i++) {
			Long id = ids[i];
			if (id == null)
				break;
			linkValues.add(LinkDegree.of(id, cache, degrees[i]));
		}
		return linkValues;
	}

	private void collectProviderInfos(ProductSystem system, IDatabase db) {

		var defaults = new TLongLongHashMap();
		String query = "select id, f_default_provider from tbl_exchanges";
		try {
			NativeSql.on(db).query(query, r -> {
				long defprov = r.getLong(2);
				if (defprov == 0L)
					return true;
				long eid = r.getLong(1);
				defaults.put(eid, defprov);
				return true;
			});
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("Failed to collect default providers", e);
		}

		var ptable = ProcessTable.create(db);
		for (ProcessLink link : system.processLinks) {
			long defaultP = defaults.get(link.exchangeId);
			if (defaultP == link.providerId) {
				defaultProviderLinkCount++;
			}
			var products = ptable.getProviders(link.flowId);
			if (products == null || products.isEmpty())
				continue;
			if (products.size() == 1) {
				singleProviderLinkCount++;
			} else {
				multiProviderLinkCount++;
			}
		}
	}

	record LinkDegree(int degree, RootDescriptor process) {
		static LinkDegree of(long id, EntityCache cache,  int degree) {
			RootDescriptor process = cache.get(ProcessDescriptor.class, id);
			if (process == null) {
				process = cache.get(ProductSystemDescriptor.class, id);
			}
			if (process == null) {
				process = cache.get(ResultDescriptor.class, id);
			}
			return new LinkDegree(degree, process);
		}
	}

}
