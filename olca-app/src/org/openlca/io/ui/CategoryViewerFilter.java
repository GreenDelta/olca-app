package org.openlca.io.ui;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.openlca.core.application.navigation.CategoryElement;
import org.openlca.core.application.navigation.INavigationElement;
import org.openlca.core.application.navigation.ModelNavigationElement;
import org.openlca.core.model.Category;
import org.openlca.core.model.modelprovider.IModelComponent;

/**
 * The category filter for the export object selection page.
 */
class CategoryViewerFilter extends ViewerFilter {

	private Class<?> clazz;
	private String className;

	public CategoryViewerFilter(Class<?> filterClass) {
		this.clazz = filterClass;
		this.className = filterClass.getCanonicalName();
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		return isVisible((INavigationElement) element);
	}

	private boolean isVisible(INavigationElement element) {
		if (element instanceof CategoryElement) {
			Category category = (Category) element.getData();
			return category.getComponentClass().equals(className)
					&& hasModelComponents(element);
		} else if (element instanceof ModelNavigationElement) {
			IModelComponent modelComponent = (IModelComponent) element
					.getData();
			return clazz.isInstance(modelComponent);
		} else
			return hasModelComponents(element);
	}

	private boolean hasModelComponents(INavigationElement element) {
		for (INavigationElement child : element.getChildren(true)) {
			if ((child instanceof ModelNavigationElement)
					&& clazz.isInstance(child.getData()))
				return true;
			else if (hasModelComponents(child))
				return true;
		}
		return false;
	}
}