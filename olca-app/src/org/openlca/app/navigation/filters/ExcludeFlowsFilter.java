package org.openlca.app.navigation.filters;

import java.util.Arrays;
import java.util.EnumSet;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.openlca.app.navigation.elements.CategoryElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.FlowDescriptor;

/// A filter for excluding flows of specific types.
public class ExcludeFlowsFilter extends ViewerFilter {

	private final EnumSet<FlowType> flowTypes;

	public ExcludeFlowsFilter(FlowType... types) {
		this.flowTypes = EnumSet.noneOf(FlowType.class);
		flowTypes.addAll(Arrays.asList(types));
	}

	@Override
	public boolean select(Viewer viewer, Object parent, Object element) {
		var tree = (TreeViewer) viewer;
		if (element instanceof ModelElement me) {
			return select(me);
		} else if (element instanceof CategoryElement ce) {
			if (filterEmptyCategories(tree)) {
				var category = ce.getContent();
				if (category.modelType == ModelType.FLOW) {
					return containsOtherTypes(ce);
				}
			}
		}
		return true;
	}

	private boolean select(ModelElement e) {
		if (!(e.getContent() instanceof FlowDescriptor d))
			return true;
		return d.flowType == null || !flowTypes.contains(d.flowType);
	}

	private boolean containsOtherTypes(CategoryElement parent) {
		for (var child : parent.getChildren()) {
			if (child instanceof ModelElement e && select(e))
				return true;
			else if (child instanceof CategoryElement e && containsOtherTypes(e))
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
