/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IInputSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.util.Colors;

/**
 * A combo viewer with tool tip support. This widget does NOT extends
 * {@link ComboViewer}, instead it supports an own implementation build on a
 * text widget and a table viewer as drop down menu
 */
public class ToolTipComboViewer extends Composite implements
		IInputSelectionProvider {

	private IContentProvider contentProvider;
	private Button dropDownButton;
	private Object input;

	/**
	 * Indicates if the drop down menu is opened
	 */
	private boolean isOpen = false;

	/**
	 * The label provider of the table viewer
	 */
	private ColumnLabelProvider labelProvider;

	/**
	 * List of {@link ISelectionChangedListener}
	 */
	private final List<ISelectionChangedListener> listener = new ArrayList<>();

	/**
	 * The actual selection
	 */
	private ISelection selection = StructuredSelection.EMPTY;

	/**
	 * The sorter of the table viewer
	 */
	private ViewerSorter sorter;

	/**
	 * Text widget displaying the actual selection
	 */
	private Text text;

	/**
	 * The viewer that appears on drop down
	 */
	private TableViewer viewer;

	/**
	 * Creates a new tool tip combo viewer
	 * 
	 * @param parent
	 *            The parent composite
	 * @param style
	 *            additional styles
	 */
	public ToolTipComboViewer(final Composite parent, final int style) {
		super(parent, SWT.BORDER | style);
		createContent();
		initListener();
	}

	/**
	 * Creates the content
	 */
	private void createContent() {
		final GridLayout layout = new GridLayout(2, false);
		layout.horizontalSpacing = 0;
		layout.marginBottom = 0;
		layout.marginHeight = 0;
		layout.marginLeft = 0;
		layout.marginRight = 0;
		layout.marginTop = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 0;
		super.setLayout(layout);
		text = new Text(this, SWT.NONE);
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		text.setEditable(false);
		text.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		dropDownButton = new Button(this, SWT.ARROW | SWT.DOWN);
	}

	/**
	 * Sorts the input with the sorter if available
	 * 
	 * @return The sorted input as list
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private List<Object> getSortedObjects() {
		List<Object> objects = null;
		if (input instanceof Collection)
			objects = new ArrayList<>((Collection) input);
		if (input instanceof Object[]) {
			objects = new ArrayList<>();
			for (Object o : (Object[]) input) {
				objects.add(o);
			}
		}
		if (sorter != null && objects != null) {
			Collections.sort(objects, new Comparator<Object>() {

				@Override
				public int compare(final Object o1, final Object o2) {
					return sorter.compare(null, o1, o2);
				}

			});
		}
		return objects;
	}

	/**
	 * Initializes the listeners
	 */
	private void initListener() {
		dropDownButton.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseDown(final MouseEvent e) {
				if (!isOpen) {
					new PopupList().open();
				} else {
					isOpen = false;
				}
			}

		});

		text.addMouseWheelListener(new MouseWheelListener() {

			@Override
			public void mouseScrolled(final MouseEvent e) {
				final List<Object> objects = getSortedObjects();
				final int selectionIndex = getSelectionIndex();
				StructuredSelection newSelection = null;
				if (e.count < 0) {
					// scrolled down
					if (objects.size() > selectionIndex + 1) {
						newSelection = new StructuredSelection(objects
								.get(selectionIndex + 1));
						setSelection(newSelection);
					}
				} else if (e.count > 0) {
					// scrolled up
					if (selectionIndex > 0) {
						newSelection = new StructuredSelection(objects
								.get(selectionIndex - 1));
						setSelection(newSelection);
					}
				}
				if (newSelection != null) {
					for (final ISelectionChangedListener l : listener) {
						l.selectionChanged(new SelectionChangedEvent(
								ToolTipComboViewer.this, newSelection));
					}
				}
			}
		});
	}

	@Override
	protected void checkSubclass() {
		// disable check for subclassing
	}

	@Override
	public void addSelectionChangedListener(
			final ISelectionChangedListener listener) {
		this.listener.add(listener);
	}

	@Override
	public Object getInput() {
		return input;
	}

	@Override
	public ISelection getSelection() {
		return selection;
	}

	/**
	 * Looks up the sorted input values for the index of the actual selection
	 * 
	 * @return The index of the actual selection in the sorted input array
	 */
	public int getSelectionIndex() {
		int index = -1;
		if (selection != null && !selection.isEmpty()
				&& selection instanceof IStructuredSelection) {
			final List<Object> objects = getSortedObjects();
			for (int i = 0; i < objects.size(); i++) {
				if (objects.get(i).equals(
						((IStructuredSelection) selection).getFirstElement())) {
					index = i;
					break;
				}
			}
		}
		return index;
	}

	@Override
	public void removeSelectionChangedListener(
			final ISelectionChangedListener listener) {
		this.listener.remove(listener);
	}

	/**
	 * Sets the content provider
	 * 
	 * @param contentProvider
	 *            The content provider
	 */
	public void setContentProvider(final IContentProvider contentProvider) {
		this.contentProvider = contentProvider;
	}

	/**
	 * Sets the input
	 * 
	 * @param input
	 *            The input
	 */
	public void setInput(Object input) {
		this.input = input;
	}

	/**
	 * Sets the label provider
	 * 
	 * @param labelProvider
	 *            The label provider
	 */
	public void setLabelProvider(final ColumnLabelProvider labelProvider) {
		this.labelProvider = labelProvider;
	}

	@Override
	public void setLayout(final Layout layout) {
		// no user defined layout allowed
	}

	@Override
	public void setSelection(final ISelection selection) {
		this.selection = selection;
		if (labelProvider != null) {
			if (selection instanceof IStructuredSelection
					&& !selection.isEmpty()) {
				text.setText(labelProvider
						.getText(((IStructuredSelection) selection)
								.getFirstElement()));
				text.setToolTipText(labelProvider
						.getToolTipText(((IStructuredSelection) selection)
								.getFirstElement()));
			} else {
				text.setText("");
			}
		}
	}

	/**
	 * Sets the sorter
	 * 
	 * @param sorter
	 *            The sorter
	 */
	public void setSorter(final ViewerSorter sorter) {
		this.sorter = sorter;
	}

	/**
	 * A popup list displaying the input as a table viewer
	 */
	private class PopupList extends PopupDialog {

		public PopupList() {
			super(ToolTipComboViewer.this.getShell(), SWT.NONE, true, true,
					true, false, false, null, null);
		}

		@Override
		protected Control createContents(final Composite parent) {
			final Control control = super.createContents(parent);
			applyBackgroundColor(Colors.getWhite(), control);
			return control;
		}

		@Override
		protected Control createDialogArea(final Composite parent) {
			// create body
			final Composite body = new Composite(parent, SWT.NONE);
			final GridLayout layout = new GridLayout();
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			body.setLayout(layout);
			body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

			// create drop down list
			viewer = new TableViewer(body, SWT.SINGLE | SWT.FULL_SELECTION);
			viewer.getTable().setLayoutData(
					new GridData(SWT.FILL, SWT.FILL, true, true));
			if (labelProvider != null) {
				viewer.setLabelProvider(labelProvider);
			}
			if (contentProvider != null) {
				viewer.setContentProvider(contentProvider);
			}
			if (sorter != null) {
				viewer.setSorter(sorter);
			}
			if (input != null) {
				viewer.setInput(input);
			}
			if (selection != null) {
				viewer.setSelection(selection);
			}

			// enabled tool tip support
			ColumnViewerToolTipSupport.enableFor(viewer);

			// add selection changed listener
			viewer.addSelectionChangedListener(new ISelectionChangedListener() {

				@Override
				public void selectionChanged(final SelectionChangedEvent arg0) {
					final IStructuredSelection selection = (IStructuredSelection) arg0
							.getSelection();
					setSelection(selection);
					close();
					for (final ISelectionChangedListener listener : ToolTipComboViewer.this.listener) {
						listener.selectionChanged(arg0);
					}
				}
			});

			return body;
		}

		@Override
		protected Point getDefaultLocation(final Point initialSize) {
			return getInitialLocation(initialSize);
		}

		@Override
		protected Point getDefaultSize() {
			return getInitialSize();
		}

		@Override
		protected Point getInitialLocation(final Point initialSize) {
			final Point location = toDisplay(0, 0);
			location.x -= 2;
			location.y += ToolTipComboViewer.this.getSize().y - 2;
			return location;
		}

		@Override
		protected Point getInitialSize() {
			return new Point(ToolTipComboViewer.this.getSize().x, 100);
		}

		@Override
		public boolean close() {
			isOpen = false;
			return super.close();
		}

		@Override
		public int open() {
			isOpen = true;
			return super.open();
		}

	}
}
