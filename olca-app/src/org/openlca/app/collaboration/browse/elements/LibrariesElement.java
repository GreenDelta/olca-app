package org.openlca.app.collaboration.browse.elements;

import java.util.List;
import java.util.stream.Collectors;

import org.openlca.collaboration.model.LibraryInfo;

public class LibrariesElement extends ServerNavigationElement<List<LibraryInfo>> {

	public LibrariesElement(IServerNavigationElement<?> parent, List<LibraryInfo> libraries) {
		super(parent, libraries);
	}

	@Override
	public boolean hasChildren() {
		return true;
	}

	@Override
	protected List<IServerNavigationElement<?>> queryChildren() {
		return getContent().stream()
				.map(lib -> new LibraryElement(this, lib))
				.collect(Collectors.toList());
	}

}
