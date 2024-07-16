package org.openlca.app.collaboration.navigation.elements;

import java.util.ArrayList;
import java.util.List;

import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.NavigationElement;
import org.openlca.collaboration.model.Entry;

public class LibrariesElement extends NavigationElement<Void>
		implements IRepositoryNavigationElement<Void> {

	public LibrariesElement(INavigationElement<?> parent) {
		super(parent, null);
	}

	@Override
	public boolean hasChildren() {
		return true;
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
		var children = new ArrayList<INavigationElement<?>>();
		WebRequests.execute(
				() -> getServer().browse(getRepositoryId(), "LIBRARY"), new ArrayList<Entry>()).stream()
				.map(e -> new EntryElement(this, e))
				.forEach(children::add);
		return children;
	}

}
