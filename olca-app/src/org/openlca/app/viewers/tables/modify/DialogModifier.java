package org.openlca.app.viewers.tables.modify;

public abstract class DialogModifier<T> implements ICellModifier<T>{

	@Override
	public boolean canModify(T element) {
		return true;
	}

	@Override
	public Object getValue(T element) {
		return null;
	}

	@Override
	public void modify(T element, Object value) {
		openDialog(element);
	}

	@Override
	public Object[] getValues(T element) {
		return null;
	}

	@Override
	public String[] getStringValues(T element) {
		return null;
	}

	@Override
	public CellEditingType getCellEditingType() {
		return CellEditingType.CHECKBOX;
	}

	@Override
	public boolean affectsOtherElements() {
		return false;
	}

	protected abstract void openDialog(T element);
	
}
