package org.openlca.app.viewers.tables.modify;

public abstract class DoubleCellModifier<T> implements ICellModifier<T> {

	@Override
	public boolean canModify(T element) {
		return element != null;
	}

	@Override
	public final ICellModifier.CellEditingType getCellEditingType() {
		return CellEditingType.TEXTBOX;
	}

	@Override
	public Object getValue(T element) {
		return getDouble(element);
	}

	@Override
	public void modify(T element, Object value) {
		if (value == null) {
			setDouble(element, null);
			return;
		}
		if (value instanceof Double) {
			setDouble(element, (Double) value);
			return;
		}
		try {
			var s = value.toString();
			var d = Double.parseDouble(s);
			setDouble(element, d);
		} catch (NumberFormatException $) {
			setDouble(element, null);
		}
	}

	public abstract Double getDouble(T element);

	public abstract void setDouble(T element, Double value);

}
