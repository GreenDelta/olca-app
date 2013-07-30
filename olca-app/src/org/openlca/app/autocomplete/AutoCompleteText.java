/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.autocomplete;

import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.BaseLabelProvider;
import org.openlca.app.BaseNameSorter;

/**
 * Widget for context assistance
 * 
 * @author Sebastian Greve
 * 
 */
public abstract class AutoCompleteText extends Text {

	/**
	 * The title of the context assistance window
	 */
	private String popupTitle;

	/**
	 * Determines if STRG/CTRL is pressed
	 */
	private boolean strgPressed = false;

	/**
	 * Creates a new context assistance text widget
	 * 
	 * @param parent
	 *            the parent composite
	 * @param popupTitle
	 *            the title of the assistance window
	 */
	public AutoCompleteText(final Composite parent, final String popupTitle) {
		super(parent, SWT.BORDER);
		this.popupTitle = popupTitle;
		addKeyListener(new KeyListener() {

			@Override
			public void keyPressed(final KeyEvent e) {
				if (e.keyCode == 262144) {
					// control pressed
					setEditable(false);
					strgPressed = true;
				}
				if (e.keyCode == 32 && strgPressed) {
					// control and whitespace pressed
					openContentAssistent();
				}
			}

			@Override
			public void keyReleased(final KeyEvent e) {
				if (e.keyCode == 262144) {
					// control released
					strgPressed = false;
					setEditable(true);
				}
			}
		});
	}

	/**
	 * Open the content assistance window
	 */
	private void openContentAssistent() {
		int cursorPosition = getCaretPosition();
		boolean cursorIsAtBeginningOfSelection = false;
		String temp = getText() == null ? "" : getText();
		if (getSelectionCount() > 0) {
			// delete selected text in String oldText
			boolean deleted = false;
			final String selectedText = getSelectionText();
			if (temp.length() >= selectedText.length() + cursorPosition) {
				if (temp.substring(cursorPosition,
						cursorPosition + selectedText.length()).equals(
						selectedText)) {
					cursorIsAtBeginningOfSelection = true;
					if (cursorPosition > 0) {
						temp = temp.substring(0, cursorPosition)
								+ temp.substring(cursorPosition
										+ selectedText.length());
					} else {
						temp = temp.substring(cursorPosition
								+ selectedText.length());
					}
					deleted = true;
				}
			}
			if (!deleted) {
				if (cursorPosition <= temp.length()) {
					temp = temp.substring(0,
							cursorPosition - selectedText.length())
							+ temp.substring(cursorPosition);
				} else {
					temp = temp.substring(0,
							cursorPosition - selectedText.length());
				}
			}
		}

		final String oldText = temp;
		if (!cursorIsAtBeginningOfSelection) {
			cursorPosition -= getSelectionCount();
		}
		final int oldCursorPosition = cursorPosition;

		// create content assistant
		final PopupDialog p = new PopupDialog(getShell(), SWT.NONE, true, true,
				true, false, false, popupTitle, null) {

			@Override
			protected Control createDialogArea(final Composite parent) {
				final Composite c = (Composite) super.createDialogArea(parent);
				c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
				c.setLayout(new GridLayout());

				// create content list
				final ListViewer list = new ListViewer(c,
						getInput().length < 10 ? SWT.SINGLE : SWT.SINGLE
								| SWT.V_SCROLL);
				list.setContentProvider(getContentProvider());
				list.setLabelProvider(getLabelProvider());
				list.setSorter(getSorter());
				list.getList().setLayoutData(
						new GridData(SWT.FILL, SWT.FILL, true, true));
				list.setInput(getInput());
				list.getList().select(0);

				// add double click listener
				list.addDoubleClickListener(new IDoubleClickListener() {

					@Override
					public void doubleClick(final DoubleClickEvent event) {
						if (!list.getSelection().isEmpty()) {
							// set selection and close content assistant
							selectionOccured(((IStructuredSelection) list
									.getSelection()).getFirstElement(),
									oldText, oldCursorPosition);
							close();
						}
					}
				});
				// add key listener
				list.getList().addKeyListener(new KeyAdapter() {

					@Override
					public void keyPressed(final KeyEvent e) {
						if (e.keyCode == 13) {
							// enter pressed
							if (!list.getSelection().isEmpty()) {
								selectionOccured(((IStructuredSelection) list
										.getSelection()).getFirstElement(),
										oldText, oldCursorPosition);
								close();
							}
						}
					}

				});
				return c;
			}

			@Override
			protected Point getInitialLocation(final Point initialSize) {
				final Point location = toDisplay(getLocation());
				location.y += getSize().y;
				return location;
			}

			@Override
			protected Point getInitialSize() {
				return new Point(400,
						getInput().length < 10 ? getInput().length * 15 + 33
								: 183);
			}

			@Override
			public boolean close() {
				setEditable(true);
				return super.close();
			}

		};
		p.open();
	}

	/**
	 * Adds the selected objects text to the original text of the widget
	 * 
	 * @param selectedElement
	 *            the object which was selected
	 * @param oldText
	 *            the old widget text
	 * @param oldCursorPosition
	 *            the old cursor position in the text widget
	 */
	private void selectionOccured(final Object selectedElement,
			final String oldText, final int oldCursorPosition) {
		final String additionalText = getTextFromListElement(selectedElement);
		String newText = "";
		if (oldText.length() > 0) {
			newText = oldText.substring(0, oldCursorPosition);
		}
		newText += additionalText;
		if (oldText.length() > oldCursorPosition) {
			newText += oldText.substring(oldCursorPosition);
		}
		setText(newText);
		setSelection(oldCursorPosition + additionalText.length());
	}

	@Override
	protected void checkSubclass() {
		// disabled for sub classing
	}

	/**
	 * Getter of the content provider
	 * 
	 * @return the content provider for the content assistance element list
	 */
	protected IContentProvider getContentProvider() {
		return new ArrayContentProvider();
	}

	/**
	 * Getter of the input of the content assistance element list
	 * 
	 * @return the input of the content assistance element list as array of
	 *         objects
	 */
	protected abstract Object[] getInput();

	/**
	 * Getter of the label provider
	 * 
	 * @return The label provider of the assistance element list
	 */
	protected ILabelProvider getLabelProvider() {
		return new BaseLabelProvider();
	}

	/**
	 * Getter of the sorter
	 * 
	 * @return the sorter for the content assistance element list
	 */
	protected ViewerSorter getSorter() {
		return new BaseNameSorter();
	}

	/**
	 * Converts an element of the content list to a String
	 * 
	 * @param element
	 *            the list element
	 * @return the string for the list element
	 */
	protected abstract String getTextFromListElement(Object element);

	@Override
	public void dispose() {
		super.dispose();
		popupTitle = null;
	}

	@Override
	public void setEditable(final boolean editable) {
		final Color color = getBackground();
		super.setEditable(editable);
		if (!editable) {
			setBackground(color);
		}
	}

}
