package org.openlca.app.navigation.elements;

import java.util.Collections;
import java.util.List;

import org.openlca.app.db.Cache;
import org.openlca.app.db.Libraries;
import org.openlca.core.model.descriptors.RootDescriptor;

public class ModelElement extends NavigationElement<RootDescriptor> {

	public ModelElement(INavigationElement<?> parent, RootDescriptor d) {
		super(parent, d);
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
		return Collections.emptyList();
	}

	public boolean isFromLibrary() {
		return Libraries.isFrom(getContent());
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
