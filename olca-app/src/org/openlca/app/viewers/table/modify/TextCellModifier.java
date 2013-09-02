package org.openlca.app.viewers.table.modify;

public abstract class TextCellModifier<T> implements ICellModifier<T> {

	@Override
	public boolean canModify(T element) {
		return true;
	}

	@Override
	public final ICellModifier.CellEditingType getCellEditingType() {
		return CellEditingType.TEXTBOX;
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
		return getText(element);
	}

	@Override
	public void modify(T element, Object value) {
		setText(element,
				value != null ? !value.toString().isEmpty() ? value.toString()
						: null : null);
	}

	protected abstract String getText(T element);

	protected abstract void setText(T element, String text);

}
