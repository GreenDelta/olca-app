package org.openlca.core.application.navigation;

import java.util.Collections;
import java.util.List;

import org.openlca.core.model.descriptors.BaseDescriptor;

public class ModelElement extends NavigationElement<BaseDescriptor> {

	public ModelElement(INavigationElement<?> parent, BaseDescriptor descriptor) {
		super(parent, descriptor);
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
		return Collections.emptyList();
	}
}
