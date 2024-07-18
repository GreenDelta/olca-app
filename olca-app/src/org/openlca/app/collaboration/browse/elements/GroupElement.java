package org.openlca.app.collaboration.browse.elements;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openlca.app.navigation.elements.Group;

public class GroupElement extends ServerNavigationElement<Group> {

	private final Map<String, Integer> counts;

	GroupElement(IServerNavigationElement<?> parent, Map<String, Integer> counts, Group group) {
		super(parent, group);
		this.counts = counts;
	}

	@Override
	protected List<IServerNavigationElement<?>> queryChildren() {
		var group = getContent();
		if (group == null)
			return Collections.emptyList();
		return Stream.of(getContent().types)
				.map(type -> EntryElement.of(this, type, counts.getOrDefault(type.name(), 0)))
				.collect(Collectors.toList());
	}

}
