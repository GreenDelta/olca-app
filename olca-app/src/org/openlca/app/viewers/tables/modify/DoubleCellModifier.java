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

}
