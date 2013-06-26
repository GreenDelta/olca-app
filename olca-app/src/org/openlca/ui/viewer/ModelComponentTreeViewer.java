/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.ui.viewer;

import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.openlca.core.application.navigation.CategoryElement;
import org.openlca.core.application.navigation.INavigationElement;
import org.openlca.core.application.navigation.ModelElement;
import org.openlca.core.application.navigation.NavigationContentProvider;
import org.openlca.core.application.navigation.NavigationLabelProvider;
import org.openlca.core.application.navigation.NavigationSorter;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;

/**
 * Tree viewer implementation for displaying categorized model components
 * 
 * @author Sebastian Greve
 * 
 */
public class ModelComponentTreeViewer extends TreeViewer {

	public ModelComponentTreeViewer(final Composite parent,
			final boolean multi, final boolean onlyCategories,
			final INavigationElement input, final ModelType modelType) {
		super(parent, SWT.BORDER | (multi ? SWT.MULTI : SWT.SINGLE));
		setContentProvider(new NavigationContentProvider());
		setLabelProvider(new NavigationLabelProvider());
		setSorter(new NavigationSorter());
		// add double click listener to expand element
		addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(final DoubleClickEvent event) {
				final IStructuredSelection selection = (IStructuredSelection) event
						.getSelection();
				if (selection.getFirstElement() instanceof CategoryElement) {
					if (isExpandable(selection.getFirstElement())) {
						setExpandedState(selection.getFirstElement(),
								getExpandedState(selection.getFirstElement()));
					}
				}
			}
		});
		// add filter
		addFilter(new ViewerFilter() {

			@Override
			public boolean select(final Viewer viewer,
					final Object parentElement, final Object element) {
				boolean select = true;
				if (element instanceof ModelElement && onlyCategories) {
					select = false;
				} else if (element instanceof CategoryElement) {
					CategoryElement e = (CategoryElement) element;
					select = onlyCategories || hasModelComponents(e);
					Category category = e.getContent();
					if (modelType != null) {
						if (category.getModelType() != modelType) {
							select = false;
						}
					}
				}
				return select;
			}
		});
		setInput(input);
		ColumnViewerToolTipSupport.enableFor(this);
	}

	/**
	 * Looks up the category element for model components
	 * 
	 * @param element
	 *            The element to look up
	 * @return True if the category element contains model components
	 */
	private boolean hasModelComponents(final CategoryElement element) {
		boolean has = false;
		for (final INavigationElement child : element.getChildren()) {
			if (child instanceof ModelElement) {
				has = true;
			} else {
				has = hasModelComponents((CategoryElement) child);
			}
			if (has) {
				break;
			}
		}
		return has;
	}
}
