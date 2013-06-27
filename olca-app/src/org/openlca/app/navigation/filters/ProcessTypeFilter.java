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
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessType;

/**
 * Abstract filter for filtering a specific process type
 * 
 * @author Sebastian Greve
 * 
 */
public abstract class ProcessTypeFilter extends ViewerFilter {

	/**
	 * The process type to filter
	 */
	private final ProcessType type;

	/**
	 * Creates a new instance
	 * 
	 * @param type
	 *            The process type to filter
	 */
	public ProcessTypeFilter(final ProcessType type) {
		this.type = type;
	}

	/**
	 * Looks up the specified category element if it contains an element with
	 * the specified process type
	 * 
	 * @param element
	 *            The category element to look up
	 * @return True if it contains at least one process element with the
	 *         specified process types
	 */
	private boolean containsProcessType(final CategoryElement element) {
		boolean contains = false;

		// for each child
		for (final INavigationElement child : element.getChildren()) {

			// if model component element
			if (child instanceof ModelElement) {
				final ModelElement elem = (ModelElement) child;
				// data is process
				if (elem.getData() instanceof Process) {
					final Process processDescriptor = (Process) elem.getData();
					contains = isProcessType(processDescriptor);
				}
			} else {
				contains = containsProcessType((CategoryElement) child);
			}
			if (contains) {
				break;
			}
		}
		return contains;
	}

	/**
	 * Looks up the process if it has the specified process type
	 * 
	 * @param process
	 *            The process to look up
	 * @return True if the type of the process is the specified, false otherwise
	 */
	private boolean isProcessType(final Process process) {
		boolean isProcessType = false;
		if (process != null && process.getProcessType() == type) {
			isProcessType = true;
		}
		return isProcessType;
	}

	@Override
	public boolean select(final Viewer viewer, final Object parentElement,
			final Object element) {
		boolean select = true;
		final TreeViewer treeViewer = (TreeViewer) viewer;
		boolean filterEmptyCategories = false;

		// for each filter
		for (final ViewerFilter filter : treeViewer.getFilters()) {
			// if filter is "empty category"-filter
			if (filter.getClass() == EmptyCategoryFilter.class) {
				filterEmptyCategories = true;
				break;
			}
		}

		// if element is model component element
		if (element instanceof ModelElement) {
			final ModelElement elem = (ModelElement) element;
			// if element is process
			if (elem.getData() instanceof Process) {
				final Process processDescriptor = (Process) elem.getData();
				select = !isProcessType(processDescriptor);
			}
		} else if (filterEmptyCategories && element instanceof CategoryElement) {
			// if category navigation element and empty category filter is
			// enabled
			final CategoryElement elem = (CategoryElement) element;
			final Category category = (Category) elem.getData();

			// if category class is process
			if (category.getModelType() == ModelType.PROCESS) {
				// check if process with macthing type is contained
				select = !containsProcessType((CategoryElement) element);
			}
		}
		return select;

	}

}
