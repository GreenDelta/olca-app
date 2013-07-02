package org.openlca.app.navigation.filters;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.openlca.app.navigation.CategoryElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;

/**
 * The category filter for a model type.
 */
public class CategoryViewerFilter extends ViewerFilter {

	private ModelType type;

	public CategoryViewerFilter(ModelType modelType) {
		this.type = modelType;
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		return isVisible((INavigationElement<?>) element);
	}

	private boolean isVisible(INavigationElement<?> element) {
		if (element instanceof CategoryElement) {
			CategoryElement e = (CategoryElement) element;
			Category category = e.getContent();
			return category.getModelType() == type
					&& hasModelComponents(element);
		} else if (element instanceof ModelElement) {
			ModelElement e = (ModelElement) element;
			BaseDescriptor d = e.getContent();
			return d.getModelType() == type;
		} else
			return hasModelComponents(element);
	}

	private boolean hasModelComponents(INavigationElement<?> element) {
		for (INavigationElement<?> child : element.getChildren()) {
			if (child instanceof ModelElement) {
				ModelElement e = (ModelElement) child;
				BaseDescriptor d = e.getContent();
				if (d.getModelType() == type)
					return true;
			} else if (hasModelComponents(child))
				return true;
		}
		return false;
	}
}