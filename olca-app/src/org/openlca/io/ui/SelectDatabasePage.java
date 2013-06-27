/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.io.ui;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.NavigationContentProvider;
import org.openlca.app.navigation.NavigationLabelProvider;
import org.openlca.app.navigation.NavigationRoot;
import org.openlca.app.navigation.NavigationSorter;
import org.openlca.app.navigation.Navigator;
import org.openlca.core.application.navigation.DataProviderNavigationElement;
import org.openlca.core.application.navigation.DatabaseNavigationElement;
import org.openlca.core.database.IDatabase;

/**
 * Wizard page for selecting a database
 * 
 * @author Sebastian Greve
 * 
 */
public class SelectDatabasePage extends WizardPage {

	/**
	 * The selected database
	 */
	private IDatabase database;

	/**
	 * Creates a new instance
	 */
	public SelectDatabasePage() {
		super("SelectDatabasePage");
		setPageComplete(false);
		setTitle(Messages.SelectDatabasePage_SelectDatabase);
		setDescription(Messages.SelectDatabasePage_Description);
	}

	@Override
	public void createControl(final Composite parent) {
		// create body
		final Composite body = new Composite(parent, SWT.NONE);
		final GridLayout layout = new GridLayout(1, false);
		layout.verticalSpacing = 10;
		body.setLayout(layout);

		new Label(body, SWT.NONE)
				.setText(Messages.SelectDatabasePage_SelectDatabase);

		// get navigation root
		NavigationRoot root = null;
		final Navigator navigator = (Navigator) PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage()
				.findView(Navigator.ID);
		if (navigator != null) {
			root = navigator.getRoot();
		}

		// create tree viewer for selecting a database
		final TreeViewer databaseViewer = new TreeViewer(body, SWT.BORDER
				| SWT.SINGLE);
		databaseViewer.setContentProvider(new NavigationContentProvider());
		databaseViewer.setLabelProvider(new NavigationLabelProvider());
		databaseViewer.setSorter(new NavigationSorter());
		databaseViewer.getTree().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));
		databaseViewer.addFilter(new ViewerFilter() {

			@Override
			public boolean select(final Viewer viewer,
					final Object parentElement, final Object element) {
				boolean select = false;
				if (element instanceof DataProviderNavigationElement
						|| element instanceof DatabaseNavigationElement) {
					select = true;
				}
				return select;
			}
		});
		if (root != null) {
			databaseViewer.setInput(root);
		}
		databaseViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {

					@Override
					public void selectionChanged(
							final SelectionChangedEvent event) {
						if (!event.getSelection().isEmpty()) {
							final IStructuredSelection selection = (IStructuredSelection) event
									.getSelection();
							final INavigationElement element = (INavigationElement) selection
									.getFirstElement();
							if (element instanceof DatabaseNavigationElement) {
								database = element.getDatabase();
								setPageComplete(true);
							} else {
								setPageComplete(false);
							}
						}
					}
				});

		setControl(body);
	}

	/**
	 * Getter of the selected database
	 * 
	 * @return the selected database
	 */
	public IDatabase getDatabase() {
		return database;
	}
}
