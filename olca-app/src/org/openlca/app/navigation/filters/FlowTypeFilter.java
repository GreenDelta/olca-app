package org.openlca.app.navigation.filters;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.openlca.app.navigation.CategoryElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.core.model.Category;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.FlowDescriptor;

/**
 * Filter for excluding specific flow types.
 */
public class FlowTypeFilter extends ViewerFilter {

	private FlowType[] flowTypes;

	public FlowTypeFilter(FlowType... types) {
		this.flowTypes = types;
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		boolean select = true;
		TreeViewer treeViewer = (TreeViewer) viewer;
		if (element instanceof ModelElement) {
			select = !matchType((ModelElement) element);
		} else if (element instanceof CategoryElement) {
			if (filterEmptyCategories(treeViewer)) {
				CategoryElement catElem = (CategoryElement) element;
				Category category = catElem.getContent();
				if (category.getModelType() == ModelType.FLOW)
					select = containsOtherTypes(catElem);
			}
		}
		return select;
	}

	private boolean matchType(ModelElement element) {
		if (element.getContent().getModelType() != ModelType.FLOW)
			return false;
		FlowDescriptor flow = (FlowDescriptor) element.getContent();
		for (FlowType flowType : flowTypes)
			if (flow != null && flow.getFlowType() == flowType)
				return true;
		return false;
	}

	private boolean containsOtherTypes(CategoryElement element) {
		for (INavigationElement<?> child : element.getChildren()) {
			if (child instanceof ModelElement
					&& !matchType((ModelElement) child))
				return true;
			else if (child instanceof CategoryElement
					&& containsOtherTypes((CategoryElement) child))
				return true;
		}
		return false;
	}

	private boolean filterEmptyCategories(TreeViewer treeViewer) {
		for (ViewerFilter filter : treeViewer.getFilters())
			if (filter.getClass() == EmptyCategoryFilter.class)
				return true;
		return false;
	}

}
