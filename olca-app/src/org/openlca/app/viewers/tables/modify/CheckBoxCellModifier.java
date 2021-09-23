package org.openlca.app.viewers.tables.modify;

public abstract class CheckBoxCellModifier<T> implements ICellModifier<T> {

	@Override
	public final ICellModifier.CellEditingType getCellEditingType() {
		return CellEditingType.CHECKBOX;
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
