package org.openlca.core.application.navigation;

import java.util.Collections;
import java.util.List;

import org.openlca.core.model.descriptors.BaseDescriptor;

public class ModelElement implements INavigationElement {

	private BaseDescriptor descriptor;
	private INavigationElement parent;

	public ModelElement(INavigationElement parent, BaseDescriptor descriptor) {
		this.descriptor = descriptor;
		this.parent = parent;
	}

	@Override
	public Object getData() {
		return descriptor;
	}

	@Override
	public INavigationElement getParent() {
		return parent;
	}

	@Override
	public List<INavigationElement> getChildren() {
		return Collections.emptyList();
	}
}
