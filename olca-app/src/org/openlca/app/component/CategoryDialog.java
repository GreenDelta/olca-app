/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.component;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.openlca.app.navigation.CategoryElement;
import org.openlca.app.navigation.ModelTypeElement;
import org.openlca.app.navigation.NavigationTree;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;

/**
 * This dialog should be used to select an openLCA category
 * 
 */
public class CategoryDialog extends Dialog {

	private Category category;
	private final ModelType modelType;
	private final String title;

	public CategoryDialog(Shell parentShell, String title,
			ModelType modelType) {
		super(parentShell);
		this.title = title;
		this.modelType = modelType;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		new Label(composite, SWT.NONE).setText(title);

		// create category viewer
		final TreeViewer categoryViewer = NavigationTree.forSingleSelection(
				composite, modelType);
		categoryViewer.setFilters(new ViewerFilter[] { new CategoryFilter() });
		categoryViewer.getTree().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));

		// add category selection changed listener
		categoryViewer
				.addSelectionChangedListener(new CategorySelectionListener());

		return parent;
	}

	@Override
	protected Point getInitialSize() {
		return new Point(400, 400);
	}

	public Category getSelectedCategory() {
		return category;
	}

	private class CategoryFilter extends ViewerFilter {

		@Override
		public boolean select(Viewer viewer, Object parentElement,
				Object element) {
			return element instanceof CategoryElement
					|| element instanceof ModelTypeElement;
		}

	}

	private class CategorySelectionListener implements
			ISelectionChangedListener {

		@Override
		public void selectionChanged(final SelectionChangedEvent event) {
			category = !event.getSelection().isEmpty() ? ((CategoryElement) ((IStructuredSelection) event
					.getSelection()).getFirstElement()).getContent() : null;
		}

	}

}
