/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.ui;

import java.util.UUID;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.openlca.core.application.Messages;
import org.openlca.core.application.navigation.CategoryNavigationElement;
import org.openlca.core.application.navigation.INavigationElement;
import org.openlca.core.application.navigation.NavigationRoot;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Category;
import org.openlca.core.model.Process;
import org.openlca.core.model.modelprovider.IModelComponent;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.viewer.ModelComponentTreeViewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This dialog should be used to select an openLCA category
 * 
 */
public class SelectCategoryDialog extends Dialog {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	/**
	 * The selected category
	 */
	private Category category;

	/**
	 * Only categories matching the component class will be displayed
	 */
	private final Class<? extends IModelComponent> componentClass;

	/**
	 * The database
	 */
	private final IDatabase database;

	/**
	 * The navigation root containing the categories
	 */
	private final NavigationRoot root;

	/**
	 * The title of the dialog
	 */
	private final String title;

	/**
	 * Creates a new dialog
	 * 
	 * @param parentShell
	 *            The parent shell
	 * @param title
	 *            The title of the dialog
	 * @param componentClass
	 *            Only categories matching the component class will be displayed
	 * @param database
	 *            The database
	 * @param root
	 *            The navigation root containing the categories
	 */
	public SelectCategoryDialog(final Shell parentShell, final String title,
			final Class<? extends IModelComponent> componentClass,
			final IDatabase database, final NavigationRoot root) {
		super(parentShell);
		this.root = root;
		this.title = title;
		this.database = database;
		this.componentClass = componentClass;
	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		// create body
		final Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		new Label(composite, SWT.NONE).setText(title);

		// create category viewer
		final TreeViewer categoryViewer = new ModelComponentTreeViewer(
				composite, false, true, root.getCategoryRoot(Process.class,
						database).getParent(), Process.class);
		categoryViewer.getTree().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));

		// add category selection changed listener
		categoryViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {

					@Override
					public void selectionChanged(
							final SelectionChangedEvent event) {
						category = !event.getSelection().isEmpty() ? (Category) ((CategoryNavigationElement) ((IStructuredSelection) event
								.getSelection()).getFirstElement()).getData()
								: null;
					}
				});

		// create popup menu
		final Menu menu = new Menu(categoryViewer.getTree());
		final MenuItem item = new MenuItem(menu, SWT.NONE);
		// add selection listener to menu item
		item.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(final SelectionEvent e) {
				// no default behaviour
			}

			@Override
			public void widgetSelected(final SelectionEvent e) {
				final IStructuredSelection selection = (IStructuredSelection) categoryViewer
						.getSelection();
				// if selection is not empty
				if (!selection.isEmpty()) {
					// cast
					final CategoryNavigationElement element = (CategoryNavigationElement) selection
							.getFirstElement();
					// get category
					final Category parent = (Category) element.getData();
					// create input dialog for new name
					final InputDialog dialog = new InputDialog(UI.shell(),
							Messages.NewCategoryDialogTitle,
							Messages.NewCategoryDialogText,
							Messages.NewCategoryDialogDefault, null);
					final int rc = dialog.open();
					if (rc == Window.OK) {
						// create category
						final Category category = new Category();
						category.setName(dialog.getValue());
						category.setId(UUID.randomUUID().toString());
						category.setParentCategory(parent);
						category.setComponentClass(componentClass
								.getCanonicalName());
						parent.add(category);

						// update parent category
						try {
							database.update(parent);
						} catch (final Exception ex) {
							log.error("Updating parent in db failed", e);
						}

						SelectCategoryDialog.this.category = category;
						categoryViewer.refresh(element);
						// for each child element
						for (final INavigationElement child : element
								.getChildren(false)) {
							// child element is the new created category
							if (child instanceof CategoryNavigationElement
									&& child.getData().equals(category)) {
								// set seleted
								categoryViewer
										.setSelection(new StructuredSelection(
												child));
								break;
							}
						}
					}
				}
			}
		});
		item.setText(Messages.AddCategoryText);
		item.setImage(ImageType.ADD_ICON.get());
		categoryViewer.getTree().setMenu(menu);
		return parent;
	}

	@Override
	protected Point getInitialSize() {
		return new Point(400, 400);
	}

	/**
	 * Getter of the selected category
	 * 
	 * @return The selected category
	 */
	public Category getSelectedCategory() {
		return category;
	}

}
