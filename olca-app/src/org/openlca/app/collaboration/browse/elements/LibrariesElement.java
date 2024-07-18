package org.openlca.app.collaboration.browse.elements;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.collaboration.model.Entry;

public class LibrariesElement extends ServerNavigationElement<Void> {

	public LibrariesElement(IServerNavigationElement<?> parent) {
		super(parent, null);
	}

	@Override
	public boolean hasChildren() {
		return true;
	}

	@Override
	protected List<IServerNavigationElement<?>> queryChildren() {
		return WebRequests.execute(
				() -> getClient().browse(getRepositoryId(), "LIBRARY"), new ArrayList<Entry>()).stream()
				.map(e -> new EntryElement(this, e))
				.collect(Collectors.toList());
	}

}
