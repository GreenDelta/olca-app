package org.openlca.app.viewers.tables.modify;

import org.eclipse.swt.SWT;

public interface ICellModifier<T> {

	enum CellEditingType {
		TEXTBOX, COMBOBOX, CHECKBOX
	}

	default boolean canModify(T element) {
		return element != null;
	}

	Object getValue(T element);

	void modify(T element, Object value);

	/**
	 * Only valid for combo-boxes: returns the list of items that can be
	 * selected for the given row element.
	 */
	default Object[] getValues(T element) {
		return null;
	}

	/**
	 * Only valid for combo-boxes: returns the list of labels for the items that
	 * can be selected for the given element. This list must be in the same
	 * order as the list given by the method {@link #getValues(Object)}.
	 */
	default String[] getStringValues(T element) {
		return null;
	}

	CellEditingType getCellEditingType();

	default boolean affectsOtherElements() {
		return false;
	}

	default int getStyle() {
		return SWT.NONE;
	}

}
