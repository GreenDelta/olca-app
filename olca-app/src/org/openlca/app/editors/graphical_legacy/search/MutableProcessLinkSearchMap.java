package org.openlca.app.editors.graphical_legacy.search;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.util.Collection;

import org.openlca.core.model.ProcessLink;

public class MutableProcessLinkSearchMap extends ProcessLinkSearchMap {

	public MutableProcessLinkSearchMap(Collection<ProcessLink> links) {
		super(links);
	}

	public void put(ProcessLink link) {
		int index = remove(link);
		if (index == -1)
			index = getAvailableIndex();
		if (index < data.size())
			data.set(index, link);
		else
			data.add(link);
		index(link.providerId, index, providerIndex);
		index(link.processId, index, connectionIndex);
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
		remove(link.providerId, index, providerIndex);
		remove(link.processId, index, connectionIndex);
		return index;
	}

	private void remove(long id, int index,
			TLongObjectHashMap<TIntArrayList> map) {
		TIntArrayList list = map.get(id);
		if (list == null)
			return;
		list.remove(index);
		if (list.size() == 0)
			map.remove(id);
	}

}
