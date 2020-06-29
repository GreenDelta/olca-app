package org.openlca.app.navigation;

import java.util.List;
import java.util.stream.Collectors;

import org.openlca.core.library.LibraryDir;

public class LibraryDirElement extends NavigationElement<LibraryDir> {

	LibraryDirElement(INavigationElement<?> parent, LibraryDir dir) {
		super(parent, dir);
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
		return getContent()
				.getLibraries()
				.stream()
				.map(lib -> new LibraryElement(this, lib))
				.collect(Collectors.toList());
	}
}
