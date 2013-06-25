package org.openlca.core.application.views.navigator.filter;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.openlca.core.application.navigation.CategoryElement;
import org.openlca.core.application.navigation.INavigationElement;
import org.openlca.core.application.navigation.ModelElement;
import org.openlca.core.model.Category;

/**
 * A filter to remove empty categories.
 */
public final class EmptyCategoryFilter extends ViewerFilter {

	private Viewer viewer;

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		this.viewer = viewer;
		boolean select = true;
		Category category = getCategory(element);
		if (isSubCategory(category)) {
			ViewerFilter[] filters = getFilters(viewer);
			select = hasContent(element, filters);
		}
		return select;
	}

	private Category getCategory(Object element) {
		Category category = null;
		if (element instanceof CategoryElement) {
			CategoryElement catElem = (CategoryElement) element;
			if (catElem.getData() instanceof Category) {
				category = (Category) catElem.getData();
			}
		}
		return category;
	}

	private boolean isSubCategory(Category category) {
		return category != null && category.getParentCategory() != null;
	}

	private ViewerFilter[] getFilters(Viewer viewer) {
		ViewerFilter[] filters = null;
		if (viewer instanceof StructuredViewer) {
			filters = ((StructuredViewer) viewer).getFilters();
		}
		return filters == null ? new ViewerFilter[0] : filters;
	}

	/**
	 * A category element is selected when there are model components in it
	 * which make it through all filters.
	 */
	private boolean hasContent(Object element, ViewerFilter[] filters) {
		List<ModelElement> content = new ArrayList<>();
		addContent(element, content);
		if (content.size() == 0) {
			return false;
		}
		return passFilters(content, filters);
	}

	private void addContent(Object element, List<ModelElement> content) {
		if (element instanceof CategoryElement) {
			CategoryElement catElement = (CategoryElement) element;
			for (INavigationElement contentElement : catElement.getChildren()) {
				if (contentElement instanceof ModelElement) {
					content.add((ModelElement) contentElement);
				} else {
					addContent(contentElement, content);
				}
			}
		}
	}

	private boolean passFilters(List<ModelElement> content,
			ViewerFilter[] filters) {
		if (filters == null || filters.length == 0) {
			return true;
		}
		for (ModelElement element : content) {
			if (passFilters(element, filters)) {
				return true;
			}
		}
		return false;
	}

	private boolean passFilters(ModelElement element, ViewerFilter[] filters) {
		boolean select = true;
		int i = 0;
		while (select && i < filters.length) {
			ViewerFilter filter = filters[i];
			if (!(filter instanceof EmptyCategoryFilter))
				select = filter.select(viewer, element.getParent(), element);
			i++;
		}
		return select;
	}
}
