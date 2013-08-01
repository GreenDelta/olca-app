package org.openlca.app.components;

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
import org.openlca.app.Messages;
import org.openlca.app.navigation.ModelElement;
import org.openlca.app.navigation.NavigationTree;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;

public class ObjectDialog extends Dialog {

	private final ModelType modelType;
	private final List<ViewerFilter> filters = new ArrayList<>();
	protected BaseDescriptor[] multiSelection;
	protected BaseDescriptor selection;
	private boolean multiSelect;

	public ObjectDialog(Shell parentShell, ModelType modelType,
			boolean multiSelect) {
		super(parentShell);
		this.modelType = modelType;
		setShellStyle(SWT.BORDER | SWT.TITLE | SWT.APPLICATION_MODAL);
		setBlockOnOpen(true);
		this.multiSelect = multiSelect;
	}

	@Override
	protected Control createButtonBar(final Composite parent) {
		final Control c = super.createButtonBar(parent);
		c.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		parent.setBackground(Display.getCurrent().getSystemColor(
				SWT.COLOR_WHITE));
		return c;
	}

	@Override
	protected void createButtonsForButtonBar(final Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				false);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, true);
	}

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
		final TreeViewer viewer = NavigationTree.forSingleSelection(composite2,
				modelType);

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
							selection.add(((ModelElement) selected)
									.getContent());
						}
					}
					// if multiple selection is allowed
					if (multiSelect) {
						// set multi selection
						ObjectDialog.this.multiSelection = selection
								.toArray(new BaseDescriptor[selection.size()]);
					} else {
						if (!selection.isEmpty()) {
							// set first selection
							ObjectDialog.this.selection = selection.get(0);
						} else {
							ObjectDialog.this.selection = null;
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
						ObjectDialog.this.multiSelection = new BaseDescriptor[] { modelComponent };
					} else {
						ObjectDialog.this.selection = modelComponent;
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
