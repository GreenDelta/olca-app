package org.openlca.app.viewers.table.modify.field;

import java.util.function.Consumer;

import org.openlca.app.editors.IEditor;

public class StringModifier<T> extends FieldModifier<T, String> {

	public StringModifier(IEditor editor, String field) {
		super(editor, field);
	}

	public StringModifier(IEditor editor, String field, Consumer<T> onChange) {
		super(editor, field, onChange);
	}

	@Override
	protected String parseText(String value, String originalValue) {
		return value;
	}

	@Override
	protected String toText(String value) {
		return value;
	}

}
