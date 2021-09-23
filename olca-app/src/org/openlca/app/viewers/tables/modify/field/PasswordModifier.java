package org.openlca.app.viewers.tables.modify.field;

import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.openlca.app.editors.ModelEditor;

public class PasswordModifier<T> extends StringModifier<T> {

	public PasswordModifier(String field) {
		this(null, field);
	}

	public PasswordModifier(ModelEditor<?> editor, String field) {
		this(editor, field, null);
	}

	public PasswordModifier(ModelEditor<?> editor, String field, Consumer<T> onChange) {
		super(editor, field, onChange);
		style = SWT.PASSWORD;
	}
}
