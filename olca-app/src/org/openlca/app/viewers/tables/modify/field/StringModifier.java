package org.openlca.app.viewers.tables.modify.field;

import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.openlca.app.editors.ModelEditor;

public class StringModifier<T> extends TextFieldModifier<T, String> {

	public int style = SWT.NONE;

	public StringModifier(String field) {
		super(null, field);
	}

	public StringModifier(ModelEditor<?> editor, String field) {
		super(editor, field);
	}

	public StringModifier(ModelEditor<?> editor, String field, Consumer<T> onChange) {
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

	@Override
	public int getStyle() {
		return style;
	}

}
