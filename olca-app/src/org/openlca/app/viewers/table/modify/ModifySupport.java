package org.openlca.app.viewers.table.modify;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.openlca.app.components.DialogCellEditor;
import org.openlca.app.viewers.table.modify.ICellModifier.CellEditingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides an easy and type safe way to add cell editors to a table viewer. It
 * is important that the viewer is configured with column properties that are
 * used for the binding of cell modifiers. Thus, you have to call
 * <code>viewer.setColumnProperties(aStringArray)</code> <b>before</b> you
 * create the modify support.
 */
public class ModifySupport<T> {

	private Logger log = LoggerFactory.getLogger(getClass());

	private Map<String, ICellModifier<T>> cellModifiers;
	private CellEditor[] editors;
	private String[] columnProperties;
	private TableViewer viewer;

	public ModifySupport(TableViewer viewer) {
		this.viewer = viewer;
		initCellEditors();
	}

	private void initCellEditors() {
		columnProperties = (String[]) viewer.getColumnProperties();
		viewer.setCellModifier(new CellModifier());
		editors = new CellEditor[columnProperties.length];
		this.cellModifiers = new HashMap<>();
		viewer.setCellEditors(editors);
	}

	/**
	 * Binds a dialog cell editor to the given property. It is assumed that the
	 * editor directly operates on the values in the respective table and that
	 * the values are set in the respective editor.
	 */
	public void bind(String property, DialogCellEditor dialog) {
		int idx = findIndex(property);
		if (idx == -1)
			return;
		editors[idx] = dialog;
	}

	/**
	 * Binds the given getter and setter to the given table property. Null
	 * values for the getter are allowed. The setter is only called if text was
	 * changed.
	 */
	public void bind(String property, Getter<T> getter, Setter<T> setter) {
		TextCellModifier<T> modifier = new TextCellModifier<T>() {
			@Override
			protected String getText(T element) {
				if (getter == null)
					return "";
				String val = getter.getText(element);
				return val == null ? "" : val;
			}

			@Override
			protected void setText(T element, String text) {
				if (getter == null || setter == null)
					return;
				String oldVal = getter.getText(element);
				if (Objects.equals(oldVal, text))
					return;
				setter.setText(element, text);
			}
		};
		bind(property, modifier);
	}

	/**
	 * Binds the given modifier to the given property of the viewer.
	 */
	public void bind(String property, ICellModifier<T> modifier) {
		int index = findIndex(property);
		if (index == -1)
			return;
		cellModifiers.put(columnProperties[index], modifier);
		setEditor(modifier, index);
	}

	private int findIndex(String property) {
		int index = -1;
		for (int i = 0; i < columnProperties.length; i++) {
			if (Objects.equals(columnProperties[i], property)) {
				index = i;
				break;
			}
		}
		if (index == -1)
			log.warn("Property {} is not a column property", property);
		return index;
	}

	private void setEditor(ICellModifier<T> modifier, int index) {
		switch (modifier.getCellEditingType()) {
		case TEXTBOX:
			editors[index] = new TextCellEditor(viewer.getTable());
			break;
		case COMBOBOX:
			editors[index] = new ComboEditor(viewer.getTable(), new String[0]);
			break;
		case CHECKBOX:
			editors[index] = new CheckboxCellEditor(viewer.getTable());
			break;
		default:
			break;
		}
	}

	private CellEditor getCellEditor(String property) {
		int idx = findIndex(property);
		if (idx == -1)
			return null;
		return editors[idx];
	}

	private void refresh(T value) {
		for (String property : cellModifiers.keySet()) {
			ICellModifier<T> modifier = cellModifiers.get(property);
			if (modifier.getCellEditingType() == CellEditingType.COMBOBOX) {
				((ComboBoxCellEditor) getCellEditor(property))
						.setItems(modifier.getStringValues(value));
			}
		}
	}

	private class CellModifier implements
			org.eclipse.jface.viewers.ICellModifier {

		@Override
		@SuppressWarnings("unchecked")
		public boolean canModify(Object element, String property) {
			if (element == null || property == null)
				return false;
			if (cellModifiers.containsKey(property)) {
				ICellModifier<T> modifier = cellModifiers.get(property);
				return modifier != null && modifier.canModify((T) element);
			}
			CellEditor editor = getCellEditor(property);
			return editor instanceof DialogCellEditor;
		}

		@Override
		public Object getValue(Object element, String property) {
			ICellModifier<T> modifier = cellModifiers.get(property);
			if (modifier != null)
				return getModifierValue(element, modifier);
			CellEditor editor = getCellEditor(property);
			if (editor != null)
				return element;
			return null;
		}

		@SuppressWarnings("unchecked")
		private Object getModifierValue(Object element,
				ICellModifier<T> modifier) {
			T elem = (T) element;
			Object value = modifier.getValue(elem);
			switch (modifier.getCellEditingType()) {
			case TEXTBOX:
				return value != null ? value.toString() : "";
			case COMBOBOX:
				return getComboIndex(modifier, elem, value);
			case CHECKBOX:
				if (value instanceof Boolean)
					return value;
				else
					return false;
			default:
				return element;
			}
		}

		private Object getComboIndex(ICellModifier<T> modifier, T elem,
				Object value) {
			refresh(elem);
			Object[] values = modifier.getValues(elem);
			if (values == null)
				return -1;
			for (int i = 0; i < values.length; i++) {
				if (Objects.equals(values[i], value))
					return i;
			}
			return -1;
		}

		@Override
		public void modify(Object element, String property, Object value) {
			if (element instanceof Item)
				element = ((Item) element).getData();
			ICellModifier<T> modifier = cellModifiers.get(property);
			if (modifier != null) {
				T elem = setModifierValue(element, value, modifier);
				refresh(elem);
			}
			viewer.refresh(true);
		}

		@SuppressWarnings("unchecked")
		private T setModifierValue(Object element, Object value,
				ICellModifier<T> modifier) {
			T elem = (T) element;
			switch (modifier.getCellEditingType()) {
			case TEXTBOX:
				modifier.modify(elem, value.toString());
				break;
			case COMBOBOX:
				setComboValue(modifier, elem, value);
				break;
			case CHECKBOX:
				modifier.modify(elem, value);
				break;
			default:
				break;
			}
			return elem;
		}

		private void setComboValue(ICellModifier<T> modifier, T elem,
				Object value) {
			if (value instanceof Integer) {
				int index = (int) value;
				if (index == -1)
					return;
				Object[] values = modifier.getValues(elem);
				if (values == null || index >= values.length)
					return;
				modifier.modify(elem, values[index]);
			}
		}
	}

	/**
	 * Overwrites the getValue method from the JFace combo editor so that also
	 * entered strings that are elements of the respective combo-items are
	 * accepted as user input.
	 */
	private class ComboEditor extends ComboBoxCellEditor {

		public ComboEditor(Composite parent, String[] items) {
			super(parent, items);
		}

		@Override
		protected Object doGetValue() {
			Object val = super.doGetValue();
			if (!(val instanceof Integer))
				return val;
			int idx = (Integer) val;
			if (idx > -1)
				return new Integer(idx);
			String cellText = getCellText();
			return getIndexForText(cellText);
		}

		private String getCellText() {
			Control control = getControl();
			if (!(control instanceof CCombo))
				return null;
			CCombo combo = (CCombo) getControl();
			return combo.getText();
		}

		private Integer getIndexForText(String cellText) {
			if (cellText == null)
				return new Integer(-1);
			String term = cellText.trim();
			String[] items = getItems();
			for (int i = 0; i < items.length; i++) {
				if (term.equals(items[i]))
					return new Integer(i);
			}
			return new Integer(-1);
		}
	}
}
