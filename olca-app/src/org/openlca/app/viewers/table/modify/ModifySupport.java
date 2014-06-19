package org.openlca.app.viewers.table.modify;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
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
			editors[index] = new ComboBoxCellEditor(viewer.getTable(),
					new String[0]);
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
				refresh(elem);
				Object[] values = modifier.getValues(elem);
				if (values != null)
					for (int i = 0; i < values.length; i++)
						if (Objects.equals(values[i], value))
							return i;
				return -1;
			case CHECKBOX:
				if (value instanceof Boolean)
					return value;
				else
					return false;
			default:
				return element;
			}
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
			if (modifier.affectsOtherElements())
				viewer.refresh(true);
			else
				viewer.refresh(element, true);
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
				if (value instanceof Integer) {
					int index = (int) value;
					if (index != -1)
						modifier.modify(elem,
								modifier.getValues(elem)[(Integer) value]);
					else
						modifier.modify(elem, null);
				}
				break;
			case CHECKBOX:
				modifier.modify(elem, value);
				break;
			default:
				break;
			}
			return elem;
		}
	}

}
