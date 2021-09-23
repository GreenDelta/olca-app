package org.openlca.app.viewers.tables.modify.field;

import java.util.Objects;
import java.util.function.Consumer;

import org.openlca.app.editors.ModelEditor;
import org.openlca.app.util.Bean;
import org.openlca.app.viewers.tables.modify.TextCellModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TextFieldModifier<T, V> extends TextCellModifier<T> {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final String field;
	private final ModelEditor<?> editor;
	private final Consumer<T> onChange;

	public TextFieldModifier(ModelEditor<?> editor, String field) {
		this(editor, field, null);
	}

	public TextFieldModifier(ModelEditor<?> editor, String field, Consumer<T> onChange) {
		this.field = field;
		this.editor = editor;
		this.onChange = onChange;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected String getText(T element) {
		try {
			Object value = Bean.getValue(element, field);
			if (value == null)
				return null;
			return toText((V) value);
		} catch (Exception e) {
			log.error("Error getting value from bean", e);
			return null;
		}
	}

	protected abstract String toText(V value);

	@Override
	@SuppressWarnings("unchecked")
	protected void setText(T element, String text) {
		try {
			Object original = Bean.getValue(element, field);
			Object value = parseText(text, (V) original);
			if (Objects.equals(original, value))
				return;
			Bean.setValue(element, field, value);
			if (editor != null)
				editor.setDirty(true);
			if (onChange != null)
				onChange.accept(element);
		} catch (Exception e) {
			log.error("Error setting value to bean", e);
		}
	}

	protected abstract V parseText(String value, V originalValue);

}
