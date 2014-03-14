package org.openlca.app.components;

import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.viewers.BaseLabelProvider;
import org.openlca.app.viewers.BaseNameSorter;

public abstract class AutoCompleteTextCellEditor extends TextCellEditor {

	private final int column;
	private Object editedElement;
	private String popupTitle;
	private boolean strgPressed = false;
	private TableViewer viewer;
	protected Text text;

	public AutoCompleteTextCellEditor( TableViewer viewer,
			 int column,  String popupTitle) {
		super(viewer.getTable(), SWT.SINGLE);
		this.viewer = viewer;
		this.column = column;
		this.popupTitle = popupTitle;
	}

	private void openContentAssistent() {
		int cursorPosition = text.getCaretPosition();
		boolean cursorIsAtBeginningOfSelection = false;
		String temp = text.getText() == null ? "" : text.getText();
		if (text.getSelectionCount() > 0) {
			// delete selected text in String oldText
			boolean deleted = false;
			final String selectedText = text.getSelectionText();
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
			cursorPosition -= text.getSelectionCount();
		}
		final int oldCursorPosition = cursorPosition;

		// create content assistant
		final PopupDialog p = new PopupDialog(text.getShell(), SWT.NONE, true,
				true, true, false, false, popupTitle, null) {

			@Override
			protected Control createDialogArea(final Composite parent) {
				editedElement = ((IStructuredSelection) viewer.getSelection())
						.getFirstElement();
				// create body
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
							selectionOccured(((IStructuredSelection) list
									.getSelection()).getFirstElement(),
									oldText, oldCursorPosition, editedElement);
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
										oldText, oldCursorPosition,
										editedElement);
								close();
							}
						}
					}

				});
				return c;
			}

			@Override
			protected Point getInitialLocation(final Point initialSize) {
				final Point location = text.toDisplay(viewer.getTable()
						.getLocation());
				location.y += text.getSize().y;
				return location;
			}

			@Override
			protected Point getInitialSize() {
				return new Point(400,
						getInput().length < 10 ? getInput().length * 15 + 33
								: 183);
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
	 * @param editedElement
	 *            The element being edited
	 */
	private void selectionOccured(final Object selectedElement,
			final String oldText, final int oldCursorPosition,
			final Object editedElement) {
		final String additionalText = getTextFromListElement(selectedElement);
		viewer.editElement(editedElement, column);
		String newText = "";
		if (oldText.length() > 0) {
			newText = oldText.substring(0, oldCursorPosition);
		}
		newText += additionalText;
		if (oldText.length() > oldCursorPosition) {
			newText += oldText.substring(oldCursorPosition);
		}
		text.setText(newText);
		text.setSelection(oldCursorPosition + additionalText.length());
	}

	@Override
	protected Control createControl(final Composite parent) {
		text = (Text) super.createControl(parent);
		text.addKeyListener(new KeyListener() {

			@Override
			public void keyPressed(final KeyEvent e) {
				if (e.keyCode == 262144) {
					strgPressed = true;
				}
				if (e.keyCode == 32 && strgPressed) {
					strgPressed = false;
					openContentAssistent();
				}
			}

			@Override
			public void keyReleased(final KeyEvent e) {
				if (e.keyCode == 262144) {
					strgPressed = false;
				}
			}
		});
		return text;
	}

	@Override
	protected boolean dependsOnExternalFocusListener() {
		return getClass() != AutoCompleteTextCellEditor.class;
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
	 * @return The label provider of the content assistance element list
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
		editedElement = null;
		viewer = null;
		popupTitle = null;
		text = null;
	}

	/**
	 * Getter of the edited element
	 * 
	 * @return the element edited in this text cell editor
	 */
	public Object getEditedElement() {
		return editedElement;
	}

}
