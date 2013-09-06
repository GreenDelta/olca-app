package org.openlca.app.viewers.table.modify;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Item;
import org.openlca.app.viewers.table.modify.ICellModifier.CellEditingType;

import com.google.common.base.Objects;

/**
 * Provides an easy and type safe way to add cell editors to a table viewer. It
 * is important that the viewer is configured with column properties that are
 * used for the binding of cell modifiers. Thus, you have to call
 * <code>viewer.setColumnProperties(aStringArray)</code> <b>before</b> you
 * create the modify support.
 */
public class ModifySupport<T> {

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
	 * Binds the given modifier to the given property of the viewer.
	 */
	public void bind(String property, ICellModifier<T> modifier) {
		int index = -1;
		for (int i = 0; i < columnProperties.length; i++)
			if (Objects.equal(columnProperties[i], property)) {
				index = i;
				break;
			}

		if (index == -1)
			throw new IllegalArgumentException("Property " + property
					+ " not found");
		this.cellModifiers.put(columnProperties[index], modifier);
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
		for (int i = 0; i < columnProperties.length; i++)
			if (columnProperties[i].equals(property))
				return viewer.getCellEditors()[i];
		return null;
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
			return cellModifiers.containsKey(property)
					&& cellModifiers.get(property) != null
					&& cellModifiers.get(property).canModify((T) element);
		}

		@Override
		@SuppressWarnings("unchecked")
		public Object getValue(Object element, String property) {
			ICellModifier<T> modifier = cellModifiers.get(property);
			if (modifier == null)
				return null;
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
						if (safeEquals(values[i], value))
							return i;
				return -1;
			case CHECKBOX:
				if (value instanceof Boolean)
					return value;
				else
					return false;
			default:
				return "";
			}

		}

		private boolean safeEquals(Object o1, Object o2) {
			if (o1 == null)
				return o2 == null;
			return o1 == o2 || o1.equals(o2);
		}

		@Override
		@SuppressWarnings("unchecked")
		public void modify(Object element, String property, Object value) {
			if (element instanceof Item)
				element = ((Item) element).getData();
			ICellModifier<T> modifier = cellModifiers.get(property);
			if (modifier == null)
				return;
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
			refresh(elem);
			viewer.refresh(true);
		}
	}

}
