package org.openlca.app.navigation.elements;

import java.util.ArrayList;
import java.util.List;

import org.openlca.core.model.ModelType;

public class GroupElement extends NavigationElement<Group> {

	GroupElement(INavigationElement<?> parent, Group group) {
		super(parent, group);
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
		var elements = new ArrayList<INavigationElement<?>>();
		for (ModelType type : getContent().types) {
			elements.add(new ModelTypeElement(this, type));
		}
		return elements;
	}

}
