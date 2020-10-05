package org.openlca.app.viewers.tables.modify.field;

import java.util.function.Consumer;

import org.openlca.app.editors.ModelEditor;

public class DoubleModifier<T> extends TextFieldModifier<T, Double> {

	public DoubleModifier(ModelEditor<?> editor, String field) {
		super(editor, field);
	}

	public DoubleModifier(ModelEditor<?> editor, String field, Consumer<T> onChange) {
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
