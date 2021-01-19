package org.openlca.app.navigation.elements;

import java.util.Collections;
import java.util.List;

import org.openlca.app.db.Cache;
import org.openlca.core.model.descriptors.CategorizedDescriptor;

public class ModelElement extends NavigationElement<CategorizedDescriptor> {

	public ModelElement(INavigationElement<?> parent, CategorizedDescriptor d) {
		super(parent, d);
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
		return Collections.emptyList();
	}

	@Override
	public void update() {
		var content = getContent();
		var newContent = Cache.getEntityCache().get(
			content.getClass(), content.id);
		setContent(newContent);
		super.update();
	}
}
