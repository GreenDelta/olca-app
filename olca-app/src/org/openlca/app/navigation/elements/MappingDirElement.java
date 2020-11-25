package org.openlca.app.navigation.elements;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.openlca.util.Strings;

/**
 * A navigation element that bundles a set of mapping files of a database.
 * The content of such an element is a set of names of the mapping files
 * it contains.
 */
public class MappingDirElement extends NavigationElement<Set<String>> {

	public MappingDirElement(
			INavigationElement<?> parent,
			Set<String> mappingFiles) {
		super(parent, mappingFiles);
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
		var names = getContent();
		if (names == null || names.isEmpty())
			return Collections.emptyList();
		return names.stream()
				.sorted(Strings::compare)
				.map(name -> new MappingFileElement(this, name))
				.collect(Collectors.toList());
	}
}
