package org.openlca.app.navigation.elements;

import java.io.File;
import java.util.List;

public class SdModelElement extends NavigationElement<File> {

	public SdModelElement(INavigationElement<?> parent, File dir) {
		super(parent, dir);
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
		return List.of();
	}
}
