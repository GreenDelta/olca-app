package org.openlca.app.viewers.table.modify.field;

import java.util.function.Consumer;

import org.openlca.app.editors.IEditor;

public class DoubleModifier<T> extends FieldModifier<T, Double> {

	public DoubleModifier(IEditor editor, String field) {
		super(editor, field);
	}

	public DoubleModifier(IEditor editor, String field, Consumer<T> onChange) {
		super(editor, field, onChange);
	}

	@Override
	protected Double parseText(String text, Double originalValue) {
		try {
			return Double.parseDouble(text);
		} catch (NumberFormatException | NullPointerException e) {
			return originalValue;
		}
	}

	@Override
	protected String toText(Double value) {
		return Double.toString(value);
	}

}
