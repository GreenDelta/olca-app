package org.openlca.app.navigation.elements;

import java.util.Collections;
import java.util.List;

import org.openlca.app.AppContext;
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
		var content = getContent();
		return content != null && content.isFromLibrary();
	}

	@Override
	public void update() {
		var content = getContent();
		var cache = AppContext.getEntityCache();
		if (content != null && cache != null) {
			var newContent = cache.get(content.getClass(), content.id);
			// if the model does not exist anymore (e.g. it was deleted), we keep
			// the old content; setting it to null would change the hash code of
			// this element while it is still shown in the navigation tree; the
			// element is removed when its parent is refreshed
			if (newContent != null) {
				setContent(newContent);
			}
		}
		super.update();
	}
}
