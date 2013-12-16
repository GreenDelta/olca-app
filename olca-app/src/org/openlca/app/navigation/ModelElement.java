package org.openlca.app.navigation;

import java.util.Collections;
import java.util.List;

import org.openlca.app.db.Cache;
import org.openlca.core.model.descriptors.BaseDescriptor;

public class ModelElement extends NavigationElement<BaseDescriptor> {

	public ModelElement(INavigationElement<?> parent, BaseDescriptor descriptor) {
		super(parent, descriptor);
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
		return Collections.emptyList();
	}

	@Override
	public void update() {
		BaseDescriptor content = getContent();
		BaseDescriptor newContent = Cache.getEntityCache().get(content.getClass(),
				content.getId());
		setContent(newContent);
		super.update();

	}
}
