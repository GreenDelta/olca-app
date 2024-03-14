package org.openlca.app.collaboration.navigation.elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openlca.app.navigation.elements.Group;
import org.openlca.app.navigation.elements.GroupElement;
import org.openlca.app.navigation.elements.INavigationElement;

public class RepositoryGroupElement extends GroupElement implements IRepositoryNavigationElement<Group> {

	private final Map<String, Integer> counts;

	RepositoryGroupElement(INavigationElement<?> parent, Map<String, Integer> counts, Group group) {
		super(parent, group);
		this.counts = counts;
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
		var group = getContent();
		if (group == null)
			return Collections.emptyList();
		var children = new ArrayList<INavigationElement<?>>();
		for (var type : getContent().types) {
			children.add(EntryElement.of(this, type, counts.getOrDefault(type.name(), 0)));
		}
		return children;
	}

}
