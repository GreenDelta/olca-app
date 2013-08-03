package org.openlca.app.viewers.table.modify;

public interface ICellModifier<T> {

	public enum CellEditingType {
		TEXTBOX, COMBOBOX, CHECKBOX;
	}

	boolean canModify(T element);

	Object getValue(T element);

	void modify(T element, Object value);

	Object[] getValues(T element);

	String[] getStringValues(T element);

	CellEditingType getCellEditingType();

}
