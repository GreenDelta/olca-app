package org.openlca.app.viewers.table.modify.field;

import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.openlca.app.editors.IEditor;

public class PasswordModifier<T> extends StringModifier<T> {

	public PasswordModifier(String field) {
		this(null, field);
	}

	public PasswordModifier(IEditor editor, String field) {
		this(editor, field, null);
	}

	public PasswordModifier(String field, Consumer<T> onChange) {
		this(null, field, onChange);
	}

	public PasswordModifier(IEditor editor, String field, Consumer<T> onChange) {
		super(editor, field, onChange);
		style = SWT.PASSWORD;
	}
}
