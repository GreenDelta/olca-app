package org.openlca.app.navigation;

import java.util.Collections;
import java.util.List;

import org.openlca.app.db.Cache;
import org.openlca.core.model.descriptors.CategorizedDescriptor;

public class ModelElement extends NavigationElement<CategorizedDescriptor> {

	public ModelElement(INavigationElement<?> parent, CategorizedDescriptor descriptor) {
		super(parent, descriptor);
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
		return Collections.emptyList();
	}

	@Override
	public void update() {
		CategorizedDescriptor content = getContent();
		CategorizedDescriptor newContent = Cache.getEntityCache().get(content.getClass(),
				content.id);
		setContent(newContent);
		super.update();

	}
}
