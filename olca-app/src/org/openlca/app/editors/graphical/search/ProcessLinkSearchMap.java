package org.openlca.app.editors.graphical.search;

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

	TLongObjectHashMap<TIntArrayList> providerIndex;
	TLongObjectHashMap<TIntArrayList> recipientIndex;
	ArrayList<ProcessLink> data;

	public ProcessLinkSearchMap(Collection<ProcessLink> links) {
		providerIndex = new TLongObjectHashMap<>(Constants.DEFAULT_CAPACITY,
				Constants.DEFAULT_LOAD_FACTOR, -1L);
		recipientIndex = new TLongObjectHashMap<>(Constants.DEFAULT_CAPACITY,
				Constants.DEFAULT_LOAD_FACTOR, -1L);
		data = new ArrayList<>(links);
		for (int i = 0; i < data.size(); i++) {
			ProcessLink link = data.get(i);
			index(link.getProviderId(), i, providerIndex);
			index(link.getRecipientId(), i, recipientIndex);
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
	 * provider or recipient.
	 */
	public List<ProcessLink> getLinks(long processId) {
		// because this method could be called quite often in graphical
		// presentations of product systems and there can be a lot of links
		// in some kind of product systems (e.g. from IO-databases) we do
		// not just merge the incoming and outgoing links here
		TIntHashSet intSet = new TIntHashSet(Constants.DEFAULT_CAPACITY,
				Constants.DEFAULT_LOAD_FACTOR, -1);
		TIntArrayList list = providerIndex.get(processId);
		if (list != null)
			intSet.addAll(list);
		list = recipientIndex.get(processId);
		if (list != null)
			intSet.addAll(list);
		return getLinks(intSet.iterator());
	}

	/**
	 * Returns all incoming links to the process with the given ID, i.e. all
	 * links where the process is recipient.
	 */
	public List<ProcessLink> getIncomingLinks(long processId) {
		return getLinks(processId, recipientIndex);
	}

	/**
	 * Returns all the outgoing links from the process with the given ID, i.e.
	 * all links where the process is provider.
	 */
	public List<ProcessLink> getOutgoingLinks(long processId) {
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
