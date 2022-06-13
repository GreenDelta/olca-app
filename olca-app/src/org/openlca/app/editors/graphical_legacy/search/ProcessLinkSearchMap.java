package org.openlca.app.editors.graphical_legacy.search;

import gnu.trove.impl.Constants;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openlca.core.model.ProcessLink;

/**
 * This is a data structure for searching a set of existing process links by
 * provider and recipient processes.
 */
public class ProcessLinkSearchMap {

	/**
	 * A map process-ID -> process links, where the process is a provider means
	 * it has the product output or waste input that is described by the link.
	 */
	final TLongObjectHashMap<TIntArrayList> providerIndex;

	/**
	 * A map process-ID -> process links, where the process is connected by a
	 * product input or waste output to another process.
	 */
	TLongObjectHashMap<TIntArrayList> connectionIndex;

	ArrayList<ProcessLink> data;

	public ProcessLinkSearchMap(Collection<ProcessLink> links) {
		providerIndex = new TLongObjectHashMap<>(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1L);
		connectionIndex = new TLongObjectHashMap<>(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1L);
		data = new ArrayList<>(links);
		for (int i = 0; i < data.size(); i++) {
			ProcessLink link = data.get(i);
			index(link.providerId, i, providerIndex);
			index(link.processId, i, connectionIndex);
		}
	}

	void index(long key, int val, TLongObjectHashMap<TIntArrayList> map) {
		TIntArrayList list = map.get(key);
		if (list == null) {
			list = new TIntArrayList(Constants.DEFAULT_CAPACITY, -1);
			map.put(key, list);
		}
		if (list.contains(val))
			return;
		list.add(val);
	}

	/**
	 * Returns all the links where the process with the given ID is either
	 * provider or connected with a provider.
	 */
	public List<ProcessLink> getLinks(long processId) {
		// because this method could be called quite often in graphical
		// presentations of product systems and there can be a lot of links
		// in some kind of product systems (e.g. from IO-databases) we do
		// not just merge the incoming and outgoing links here
		TIntHashSet intSet = new TIntHashSet(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1);
		TIntArrayList list = providerIndex.get(processId);
		if (list != null)
			intSet.addAll(list);
		list = connectionIndex.get(processId);
		if (list != null)
			intSet.addAll(list);
		return getLinks(intSet.iterator());
	}

	/**
	 * Returns all links where the process with the given ID is connected (has
	 * an product input or waste output).
	 */
	public List<ProcessLink> getConnectionLinks(long processId) {
		return getLinks(processId, connectionIndex);
	}

	/**
	 * Returns all links where the process with the given ID is a provider ( has
	 * a product output or waste input).
	 */
	public List<ProcessLink> getProviderLinks(long processId) {
		return getLinks(processId, providerIndex);
	}

	private List<ProcessLink> getLinks(long processId,
			TLongObjectHashMap<TIntArrayList> map) {
		TIntArrayList list = map.get(processId);
		if (list == null)
			return Collections.emptyList();
		return getLinks(list.iterator());
	}

	private List<ProcessLink> getLinks(TIntIterator iterator) {
		List<ProcessLink> links = new ArrayList<>();
		while (iterator.hasNext()) {
			ProcessLink next = data.get(iterator.next());
			if (next != null)
				links.add(next);
		}
		return links;
	}
}
