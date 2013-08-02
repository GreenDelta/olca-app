package org.openlca.app.viewers.modify;

public abstract class ComboBoxCellModifier<T, V> implements ICellModifier<T> {

	@Override
	public boolean canModify(T element) {
		return true;
	}

	@Override
	public final ICellModifier.CellEditingType getCellEditingType() {
		return CellEditingType.COMBOBOX;
	}

	@Override
	public final Object getValue(T element) {
		return getItem(element);
	}

	@Override
	public final Object[] getValues(T element) {
		return getItems(element);
	}

	@Override
	public String[] getStringValues(T element) {
		V[] values = getItems(element);
		String[] strings = new String[values.length];
		for (int i = 0; i < values.length; i++)
			strings[i] = getText(values[i]);
		return strings;
	}

	@SuppressWarnings("unchecked")
	@Override
	public final void modify(T element, Object value) {
		setItem(element, (V) value);
	}

	protected abstract V[] getItems(T element);

	protected abstract V getItem(T element);

	protected abstract String getText(V value);

	protected abstract void setItem(T element, V item);

}
