package org.openlca.app.viewers.table.modify;

public interface ICellModifier<T> {

	public enum CellEditingType {
		TEXTBOX, COMBOBOX, CHECKBOX;
	}

	boolean canModify(T element);

	Object getValue(T element);

	void modify(T element, Object value);

	/**
	 * Only valid for combo-boxes: returns the list of items that can be
	 * selected for the given row element.
	 */
	Object[] getValues(T element);

	/**
	 * Only valid for combo-boxes: returns the list of labels for the items that
	 * can be selected for the given element. This list must be in the same
	 * order as the list given by the method {@link #getValues(Object)}.
	 */
	String[] getStringValues(T element);

	CellEditingType getCellEditingType();

}
