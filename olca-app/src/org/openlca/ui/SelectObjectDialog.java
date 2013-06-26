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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.core.application.Messages;
import org.openlca.core.application.navigation.ModelElement;
import org.openlca.core.application.navigation.NavigationRoot;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.ui.viewer.ModelComponentTreeViewer;

/**
 * A dialog for the selection of a model component type.
 */
public class SelectObjectDialog extends Dialog {

	/**
	 * The class of components that can be selected
	 */
	private final ModelType modelType;

	/**
	 * Additional filters
	 */
	private final List<ViewerFilter> filters = new ArrayList<>();

	/**
	 * Indicates if multiple selection is allowed
	 */
	private final boolean multiSelect;

	/**
	 * The current selection of the table viewer.
	 */
	protected BaseDescriptor[] multiSelection;

	/**
	 * The input of this dialog.
	 */
	protected NavigationRoot root;

	/**
	 * The current first element of the table viewer.
	 */
	protected BaseDescriptor selection;

	/**
	 * Creates the dialog.
	 * 
	 * @param parentShell
	 *            see {@link Dialog#getShell()}
	 * @param root
	 *            The input of this dialog
	 * @param multiSelect
	 *            Indicates if multiple objects can be selected
	 * @param database
	 *            The database
	 * @param clazz
	 *            Only objects matching this class will be displayed, all others
	 *            will be filtered
	 */
	public SelectObjectDialog(final Shell parentShell,
			final NavigationRoot root, final boolean multiSelect,
			ModelType modelType) {
		super(parentShell);
		this.modelType = modelType;
		setShellStyle(SWT.BORDER | SWT.TITLE | SWT.APPLICATION_MODAL);
		this.root = root;
		this.multiSelect = multiSelect;
		setBlockOnOpen(true);
	}

	@Override
	protected Control createButtonBar(final Composite parent) {
		final Control c = super.createButtonBar(parent);
		c.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		parent.setBackground(Display.getCurrent().getSystemColor(
				SWT.COLOR_WHITE));
		return c;
	}

	/**
	 * Create contents of the button bar
	 * 
	 * @param parent
	 *            The parent composite
	 */
	@Override
	protected void createButtonsForButtonBar(final Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				false);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, true);
	}

	/**
	 * Create contents of the dialog
	 * 
	 * @param parent
	 *            The parent composite
	 */
	@Override
	protected Control createDialogArea(final Composite parent) {

		// create dialog container
		final Composite container = (Composite) super.createDialogArea(parent);
		final GridLayout gl = new GridLayout();
		gl.verticalSpacing = 0;
		gl.horizontalSpacing = 0;
		gl.marginRight = 0;
		gl.marginLeft = 0;
		gl.marginBottom = 0;
		gl.marginTop = 0;
		gl.numColumns = 1;
		container.setLayout(gl);
		final FormToolkit toolkit = new FormToolkit(Display.getCurrent());
		toolkit.adapt(container);

		// create section and section client
		final Section selectObjectSection = toolkit
				.createSection(container, ExpandableComposite.TITLE_BAR
						| ExpandableComposite.FOCUS_TITLE);
		selectObjectSection.setText(Messages.SelectObjectDialog_SectionText);
		selectObjectSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL,
				true, true));

		// create composite
		final Composite composite = toolkit.createComposite(
				selectObjectSection, SWT.NONE);
		final GridLayout gridLayout = new GridLayout();
		gridLayout.verticalSpacing = 10;
		gridLayout.horizontalSpacing = 10;
		gridLayout.marginRight = 10;
		gridLayout.marginLeft = 10;
		gridLayout.marginBottom = 10;
		gridLayout.marginTop = 10;
		gridLayout.numColumns = 1;
		composite.setLayout(gridLayout);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		selectObjectSection.setClient(composite);
		toolkit.paintBordersFor(composite);

		// create composite
		final Composite composite2 = toolkit.createComposite(composite);
		composite2.setLayout(new GridLayout());
		composite2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		toolkit.paintBordersFor(composite2);

		// create tree viewer for selecting objects
		final TreeViewer viewer = new ModelComponentTreeViewer(composite2,
				multiSelect, false, root, null);

		// for each filter
		for (final ViewerFilter filter : filters) {
			// add filter
			viewer.addFilter(filter);
		}

		viewer.getTree().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));

		// add a selection listener to the viewer
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				// if selection is not empty
				if (event.getSelection() != null
						&& !event.getSelection().isEmpty()) {
					// get selection
					final IStructuredSelection s = (IStructuredSelection) event
							.getSelection();
					final List<BaseDescriptor> selection = new ArrayList<>();
					// for each selected object
					for (final Object selected : s.toArray()) {
						// if object is model component element
						if (selected instanceof ModelElement) {
							// add to filtered selection
							selection
									.add((BaseDescriptor) ((ModelElement) selected)
											.getContent());
						}
					}
					// if multiple selection is allowed
					if (multiSelect) {
						// set multi selection
						SelectObjectDialog.this.multiSelection = selection
								.toArray(new BaseDescriptor[selection.size()]);
					} else {
						if (!selection.isEmpty()) {
							// set first selection
							SelectObjectDialog.this.selection = selection
									.get(0);
						} else {
							SelectObjectDialog.this.selection = null;
						}
					}
				} else if (multiSelect) {
					multiSelection = null;
				} else {
					selection = null;
				}
				getButton(IDialogConstants.OK_ID).setEnabled(
						multiSelect && multiSelection != null
								&& multiSelection.length > 0 || !multiSelect
								&& selection != null);
			}
		});

		viewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(final DoubleClickEvent event) {
				final IStructuredSelection selection = (IStructuredSelection) event
						.getSelection();
				if (selection.getFirstElement() instanceof ModelElement) {
					// set first selection
					final ModelElement element = (ModelElement) selection
							.getFirstElement();
					final BaseDescriptor modelComponent = element.getContent();
					if (multiSelect) {
						SelectObjectDialog.this.multiSelection = new BaseDescriptor[] { modelComponent };
					} else {
						SelectObjectDialog.this.selection = modelComponent;
					}
					okPressed();
				}
			}
		});
		return container;
	}

	/**
	 * Return the initial size of the dialog
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(400, 300);
	}

	/**
	 * Adds a filter to the dialog's input viewer
	 * 
	 * @param filter
	 *            The filter to add
	 */
	public void addFilter(final ViewerFilter filter) {
		filters.add(filter);
	}

	/**
	 * Returns the selected model components
	 * 
	 * @return null if no selection was made or the parameter multi select was
	 *         set to false, otherwise the selected model components
	 */
	public BaseDescriptor[] getMultiSelection() {
		return multiSelection;
	}

	/**
	 * Get the current selection of this dialog.
	 * 
	 * @return the current selection of this dialog, or <code>null</code> if no
	 *         object is selected or the parameter multi select was set to true
	 */
	public BaseDescriptor getSelection() {
		return selection;
	}

}
