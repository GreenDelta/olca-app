package org.openlca.app.viewers.tables.modify;

public abstract class TextCellModifier<T> implements ICellModifier<T> {

	@Override
	public final ICellModifier.CellEditingType getCellEditingType() {
		return CellEditingType.TEXTBOX;
	}

	@Override
	public Object getValue(T element) {
		return getText(element);
	}

	@Override
	public void modify(T element, Object value) {
		var s = value == null
			? null
			: value.toString();
		setText(element, s == null || s.isEmpty() ? null : s);
	}

	protected abstract String getText(T element);

	protected abstract void setText(T element, String text);

}
