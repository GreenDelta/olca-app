package org.openlca.app.editors.graphical.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openlca.core.model.ProcessLink;

import gnu.trove.impl.Constants;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;


/// A data structure for searching process links
public class LinkSearchMap {

	/// `provider ID -> process links`; all links indexed by their
	/// providers (product outputs or waste inputs)
	private final TLongObjectHashMap<TIntArrayList> providerIdx;

	/// `process ID -> process links`; all links indexed by their
	///  connected processes (product inputs or waste outputs)
	private final TLongObjectHashMap<TIntArrayList> consumerIdx;

	/// All links stored in a linear list; the indices point to
	/// positions of that list.
	private final ArrayList<ProcessLink> data;

	public LinkSearchMap(Collection<ProcessLink> links) {
		providerIdx = new TLongObjectHashMap<>(Constants.DEFAULT_CAPACITY,
				Constants.DEFAULT_LOAD_FACTOR, -1L);
		consumerIdx = new TLongObjectHashMap<>(Constants.DEFAULT_CAPACITY,
				Constants.DEFAULT_LOAD_FACTOR, -1L);
		data = new ArrayList<>(links);
		for (int i = 0; i < data.size(); i++) {
			var link = data.get(i);
			index(link.providerId, i, providerIdx);
			index(link.processId, i, consumerIdx);
		}
	}

	private void index(long key, int val, TLongObjectHashMap<TIntArrayList> map) {
		var list = map.get(key);
		if (list == null) {
			list = new TIntArrayList(Constants.DEFAULT_CAPACITY, -1);
			map.put(key, list);
		}
		if (!list.contains(val)) {
			list.add(val);
		}
	}

	/// Returns all links where the process (or result, or sub-system) with the
	/// given ID is used (as a provider or linked process).
	public List<ProcessLink> getAllLinks(long processId) {
		// because this method could be called quite often in graphical
		// presentations of product systems and there can be a lot of links
		// in some kind of product systems (e.g. from IO-databases) we do
		// not just merge the incoming and outgoing links here
		var intSet = new TIntHashSet(Constants.DEFAULT_CAPACITY,
				Constants.DEFAULT_LOAD_FACTOR, -1);
		var list = providerIdx.get(processId);
		if (list != null) {
			intSet.addAll(list);
		}
		list = consumerIdx.get(processId);
		if (list != null) {
			intSet.addAll(list);
		}
		return fetchLinks(intSet.iterator());
	}

	/// Returns all links where the processes (or results, or sub-systems) with
	/// the given IDs are used (as providers or linked processes).
	public List<ProcessLink> getAllLinks(List<Long> processIds) {
		var intSet = new TIntHashSet(
				Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1);
		for (long processId : processIds) {
			var list = providerIdx.get(processId);
			if (list != null) {
				intSet.addAll(list);
			}
			list = consumerIdx.get(processId);
			if (list != null) {
				intSet.addAll(list);
			}
		}
		return fetchLinks(intSet.iterator());
	}

	/// Returns all links where the process with the given ID is a consumer of
	/// a product input or waste service (has a waste output).
	public List<ProcessLink> getConsumerLinks(long processId) {
		return fetchLinks(processId, consumerIdx);
	}

	/**
	 * Returns all links where the process with the given ID is a provider ( has
	 * a product output or waste input).
	 */
	public List<ProcessLink> getProviderLinks(long processId) {
		return fetchLinks(processId, providerIdx);
	}

	private List<ProcessLink> fetchLinks(
		long id, TLongObjectHashMap<TIntArrayList> map) {
		var list = map.get(id);
		return list != null
			? fetchLinks(list.iterator())
			: List.of();
	}

	private List<ProcessLink> fetchLinks(TIntIterator it) {
	  var links = new ArrayList<ProcessLink>();
		while (it.hasNext()) {
			var next = data.get(it.next());
			if (next != null)
				links.add(next);
		}
		return links;
	}

	public void put(ProcessLink link) {
		int index = remove(link);
		if (index == -1)
			index = getAvailableIndex();
		if (index < data.size())
			data.set(index, link);
		else
			data.add(link);
		index(link.providerId, index, providerIdx);
		index(link.processId, index, consumerIdx);
	}

	private int getAvailableIndex() {
		for (int index = 0; index < data.size(); index++)
			if (data.get(index) == null) // previously removed link
				return index;
		return data.size();
	}

	public void removeAll(Collection<ProcessLink> links) {
		for (ProcessLink link : links)
			remove(link);
	}

	public int remove(ProcessLink link) {
		int index = data.indexOf(link);
		if (index < 0)
			return -1;
		data.set(index, null);
		remove(link.providerId, index, providerIdx);
		remove(link.processId, index, consumerIdx);
		return index;
	}

	private void remove(long id, int index,
			TLongObjectHashMap<TIntArrayList> map) {
		TIntArrayList list = map.get(id);
		if (list == null)
			return;
		list.remove(index);
		if (list.isEmpty())
			map.remove(id);
	}

}
