package org.openlca.app.viewers.tables.modify;

/**
 * A combo-box modifier for table cells. We have two type variables here: one
 * for the type of elements behind the rows in the respective table viewer and
 * one for the respective values in the combo-box.
 *
 * @param <R>
 *            the row type of the table
 * @param <C>
 *            the type of items in the combo-box
 */
public abstract class ComboBoxCellModifier<R, C> implements ICellModifier<R> {

	@Override
	public final ICellModifier.CellEditingType getCellEditingType() {
		return CellEditingType.COMBOBOX;
	}

	@Override
	public final Object getValue(R element) {
		return getItem(element);
	}

	@Override
	public final Object[] getValues(R element) {
		return getItems(element);
	}

	@Override
	public String[] getStringValues(R element) {
		C[] values = getItems(element);
		String[] strings = new String[values.length];
		for (int i = 0; i < values.length; i++)
			strings[i] = getText(values[i]);
		return strings;
	}

	@SuppressWarnings("unchecked")
	@Override
	public final void modify(R element, Object value) {
		setItem(element, (C) value);
	}

	protected abstract C[] getItems(R element);

	protected abstract C getItem(R element);

	protected abstract String getText(C value);

	protected abstract void setItem(R element, C item);

}
