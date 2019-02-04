package org.openlca.app.ilcd_network;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.openlca.app.navigation.CategoryElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;

/**
 * The navigation tree-filter for the ILCD network export. Allows the selection
 * of processes and product systems.
 * 
 * TODO: see filters in navigation package
 */
public class NavigationTreeFilter extends ViewerFilter {

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (element instanceof INavigationElement)
			return select((INavigationElement<?>) element);
		return false;
	}

	private boolean select(INavigationElement<?> element) {
		if (element instanceof CategoryElement)
			return validCategory((CategoryElement) element);
		if (element instanceof ModelElement)
			return validModel((ModelElement) element);
		return hasModelChilds(element);
	}

	private boolean validCategory(CategoryElement e) {
		Category c = e.getContent();
		if (c == null || c.modelType == null)
			return false;
		ModelType type = c.modelType;
		return (type.isOneOf(
				ModelType.PROCESS, ModelType.PRODUCT_SYSTEM))
				&& hasModelChilds(e);
	}

	private boolean validModel(ModelElement element) {
		BaseDescriptor model = element.getContent();
		return model.type.isOneOf(ModelType.PROCESS, ModelType.PRODUCT_SYSTEM);
	}

	private boolean hasModelChilds(INavigationElement<?> element) {
		for (INavigationElement<?> child : element.getChildren()) {
			if ((child instanceof ModelElement)
					&& validModel((ModelElement) child))
				return true;
			else if (hasModelChilds(child))
				return true;
		}
		return false;
	}

}
