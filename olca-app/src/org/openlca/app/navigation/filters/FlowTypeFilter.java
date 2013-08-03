/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
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
 * Filter for filtering specific flow types
 */
public class FlowTypeFilter extends ViewerFilter {

	private FlowType[] flowTypes;

	public FlowTypeFilter(FlowType... types) {
		this.flowTypes = types;
	}

	private boolean containsType(CategoryElement element) {
		for (INavigationElement<?> child : element.getChildren())
			if (child instanceof ModelElement
					&& matchType((ModelElement) child))
				return true;
			else if (child instanceof CategoryElement
					&& containsType((CategoryElement) child))
				return true;
		return false;
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

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		boolean select = true;
		TreeViewer treeViewer = (TreeViewer) viewer;

		// if element is model component element
		if (element instanceof ModelElement) {
			select = !matchType((ModelElement) element);
		} else if (element instanceof CategoryElement) {
			if (filterEmptyCategories(treeViewer)) {
				Category category = ((CategoryElement) element).getContent();
				if (category.getModelType() == ModelType.FLOW)
					select = !containsType((CategoryElement) element);
			}
		}

		return select;
	}

	private boolean filterEmptyCategories(TreeViewer treeViewer) {
		for (ViewerFilter filter : treeViewer.getFilters())
			if (filter.getClass() == EmptyCategoryFilter.class)
				return true;
		return false;
	}

}
