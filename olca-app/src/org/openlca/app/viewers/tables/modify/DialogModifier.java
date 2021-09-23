package org.openlca.app.viewers.tables.modify;

public abstract class DialogModifier<T> implements ICellModifier<T>{

	@Override
	public Object getValue(T element) {
		return null;
	}

	@Override
	public void modify(T element, Object value) {
		openDialog(element);
	}

	@Override
	public CellEditingType getCellEditingType() {
		return CellEditingType.CHECKBOX;
	}

	protected abstract void openDialog(T element);

}
