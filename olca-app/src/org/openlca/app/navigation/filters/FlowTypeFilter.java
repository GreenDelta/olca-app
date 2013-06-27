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
import org.openlca.app.navigation.ModelElement;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowType;

/**
 * Filter for filtering specific flow types
 * 
 * @author Sebastian Greve
 * 
 */
public class FlowTypeFilter extends ViewerFilter {

	private FlowType[] flowTypes;

	/**
	 * Creates a new instance
	 * 
	 * @param types
	 *            The flow types to filter
	 */
	public FlowTypeFilter(FlowType... types) {
		this.flowTypes = types;
	}

	// private boolean containsFlowAndMatchType(INavigationElement elem) {
	// boolean bool = false;
	// if (elem.getData() instanceof Flow) {
	// Flow flowDescriptor = (Flow) elem.getData();
	// bool = matchType(flowDescriptor);
	// }
	// return bool;
	// }

	private boolean matchType(Flow flow) {
		boolean isFlowType = false;
		for (FlowType flowType : flowTypes) {
			if (flow != null && flow.getFlowType() == flowType) {
				isFlowType = true;
				break;
			}
		}
		return isFlowType;
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		boolean select = true;
		TreeViewer treeViewer = (TreeViewer) viewer;
		// boolean filterEmptyCategories = false;

		// for each filter
		for (ViewerFilter filter : treeViewer.getFilters()) {
			// if filter is "empty category"-filter
			if (filter.getClass() == EmptyCategoryFilter.class) {
				// filterEmptyCategories = true;
				break;
			}
		}

		// if element is model component element
		if (element instanceof ModelElement) {
			ModelElement elem = (ModelElement) element;

			// // if data is flow
			// if (elem.getContent() instanceof Flow) {
			// Flow flowDescriptor = (Flow) elem.getContent();
			// select = !matchType(flowDescriptor);
			// }
		}

		// else if (filterEmptyCategories
		// && element instanceof CategoryNavigationElement) {
		// CategoryNavigationElement elem = (CategoryNavigationElement) element;
		// Category category = (Category) elem.getData();
		//
		// // if category is flow category and not top category
		// if (category.getComponentClass().equals(
		// Flow.class.getCanonicalName())
		// && !category.getComponentClass().equals(category.getId())) {
		// // check if a flow matching the flow type is contained
		// select = !containsFlowType((CategoryNavigationElement) element);
		// }
		// }
		//
		return select;

	}

}
