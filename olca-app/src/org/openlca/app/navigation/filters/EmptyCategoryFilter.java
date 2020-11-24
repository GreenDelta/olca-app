package org.openlca.app.navigation.filters;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.openlca.app.navigation.elements.CategoryElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ModelElement;

/**
 * A filter which removes empty categories.
 */
public final class EmptyCategoryFilter extends ViewerFilter {

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (!(element instanceof CategoryElement))
			return true;
		return hasContent(viewer, (CategoryElement) element);
	}

	/**
	 * A category element is selected when there are model components in it
	 * which make it through all filters.
	 */
	private boolean hasContent(Viewer viewer, CategoryElement element) {
		List<ModelElement> content = new ArrayList<>();
		collectContent(element, content);
		if (content.size() == 0)
			return false;
		ViewerFilter[] filters = getFilters(viewer);
		if (filters.length == 0)
			return true;
		for (ModelElement model : content)
			if (passesFilters(viewer, filters, model))
				return true;
		return false;
	}

	private void collectContent(CategoryElement element,
			List<ModelElement> content) {
		for (INavigationElement<?> contentElement : element.getChildren())
			if (contentElement instanceof ModelElement)
				content.add((ModelElement) contentElement);
			else
				collectContent((CategoryElement) contentElement, content);
	}

	private ViewerFilter[] getFilters(Viewer viewer) {
		ViewerFilter[] filters = null;
		if (viewer instanceof StructuredViewer)
			filters = ((StructuredViewer) viewer).getFilters();
		return filters == null ? new ViewerFilter[0] : filters;
	}

	private boolean passesFilters(Viewer viewer, ViewerFilter[] filters,
			ModelElement element) {
		for (ViewerFilter filter : filters)
			if (!filter.select(viewer, element.getParent(), element))
				return false;
		return true;
	}
}
