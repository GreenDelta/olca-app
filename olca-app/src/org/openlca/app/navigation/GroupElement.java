package org.openlca.app.navigation;

import java.util.ArrayList;
import java.util.List;

import org.openlca.core.model.ModelType;

public class GroupElement extends NavigationElement<Group> {

	public GroupElement(INavigationElement<?> parent, Group group) {
		super(parent, group);
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
		List<INavigationElement<?>> elements = new ArrayList<>();
		for (ModelType type : getContent().types)
			elements.add(new ModelTypeElement(this, type));
		return elements;
	}

}
