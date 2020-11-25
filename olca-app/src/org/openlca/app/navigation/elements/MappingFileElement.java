package org.openlca.app.navigation.elements;

import java.util.Collections;
import java.util.List;

/**
 * A navigation element that represents a mapping file in a database.
 * The content of the element is the name of the corresponding mapping
 * file.
 */
public class MappingFileElement extends NavigationElement<String> {

	public MappingFileElement(
			MappingDirElement parent,
			String mappingFile) {
		super(parent, mappingFile);
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
		return Collections.emptyList();
	}
}
