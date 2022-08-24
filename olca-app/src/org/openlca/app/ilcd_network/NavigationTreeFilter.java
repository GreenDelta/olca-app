package org.openlca.app.ilcd_network;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.openlca.app.navigation.elements.CategoryElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.core.model.ModelType;

/**
 * The navigation tree-filter for the ILCD network export. Allows the selection
 * of processes and product systems.
 * <p>
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
		if (element instanceof CategoryElement ce)
			return validCategory(ce);
		if (element instanceof ModelElement me)
			return validModel(me);
		return hasModelChilds(element);
	}

	private boolean validCategory(CategoryElement e) {
		var category = e.getContent();
		if (category == null || category.modelType == null)
			return false;
		var type = category.modelType;
		return hasModelChilds(e) && (
				type == ModelType.PROCESS || type == ModelType.PRODUCT_SYSTEM);
	}

	private boolean validModel(ModelElement element) {
		var type = element.getContent().type;
		return type == ModelType.PROCESS || type == ModelType.PRODUCT_SYSTEM;
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
