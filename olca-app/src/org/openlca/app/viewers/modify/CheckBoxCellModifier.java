package org.openlca.app.viewers.modify;


public abstract class CheckBoxCellModifier<T> implements ICellModifier<T> {

	@Override
	public boolean canModify(T element) {
		return true;
	}

	@Override
	public final ICellModifier.CellEditingType getCellEditingType() {
		return CellEditingType.CHECKBOX;
	}

	@Override
	public final Object[] getValues(T element) {
		return null;
	}

	@Override
	public String[] getStringValues(T element) {
		return null;
	}

	@Override
	public Object getValue(T element) {
		return isChecked(element);
	}

	@Override
	public void modify(T element, Object value) {
		if (value instanceof Boolean)
			setChecked(element, (Boolean) value);
	}

	protected abstract boolean isChecked(T element);

	protected abstract void setChecked(T element, boolean value);

}
