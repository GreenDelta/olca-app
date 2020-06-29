package org.openlca.app.navigation;

import java.util.Collections;
import java.util.List;

import org.openlca.core.library.Library;

public class LibraryElement extends NavigationElement<Library> {

	LibraryElement(INavigationElement<?> parent, Library library) {
		super(parent, library);
	}


	@Override
	protected List<INavigationElement<?>> queryChilds() {
		return Collections.emptyList();
	}
}
