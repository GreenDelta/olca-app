package org.openlca.app.collaboration.browse.elements;

import java.util.ArrayList;
import java.util.List;

import org.openlca.collaboration.model.LibraryInfo;

public class LibraryElement extends ServerNavigationElement<LibraryInfo> {

	public LibraryElement(IServerNavigationElement<?> parent, LibraryInfo content) {
		super(parent, content);
	}

	@Override
	public boolean hasChildren() {
		return false;
	}

	@Override
	protected List<IServerNavigationElement<?>> queryChildren() {
		return new ArrayList<>();
	}

}
