package org.openlca.app.preferencepages;

import java.util.List;
import java.util.Objects;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.openlca.app.Messages;
import org.openlca.app.components.UncertaintyCellEditor;
import org.openlca.app.util.Error;
import org.openlca.app.util.TableClipboard;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UncertaintyLabel;
import org.openlca.app.util.Viewers;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.model.Parameter;

class DatabaseParameterTable {

	private TableViewer viewer;
	private Table table;

	private final String NAME = Messages.Name;
	private final String DESCRIPTION = Messages.Description;
	private final String AMOUNT = Messages.Amount;
	private final String UNCERTAINTY = Messages.Uncertainty;
	private final String[] PROPERTIES = new String[] { NAME, AMOUNT,
			UNCERTAINTY, DESCRIPTION };

	public DatabaseParameterTable(Composite parent) {
		createTableViewer(parent);
		createEditors();
	}

	private void createTableViewer(Composite parent) {
		viewer = Tables.createViewer(parent, PROPERTIES);
		viewer.setLabelProvider(new ParameterLabel());
		table = viewer.getTable();
		table.setEnabled(false);
		Tables.bindColumnWidths(table, 0.2, 0.2, 0.3, 0.3);
	}

	private void createEditors() {
		ModifySupport<Parameter> support = new ModifySupport<>(viewer);
		support.bind(NAME, new NameModifier());
		support.bind(AMOUNT, new AmountModifier());
		support.bind(DESCRIPTION, new DescriptionModifier());
		support.bind(UNCERTAINTY, new UncertaintyCellEditor(viewer.getTable()));
	}

	public Parameter getSelected() {
		return Viewers.getFirstSelected(viewer);
	}

	public void setInput(List<Parameter> parameters) {
		viewer.setInput(parameters);
	}

	public void setActions(Action... actions) {
		MenuManager menu = new MenuManager();
		for (Action action : actions)
			menu.add(action);
		Action copy = TableClipboard.onCopy(table);
		menu.add(copy);
		table.setMenu(menu.createContextMenu(table));
	}

	public void setEnabled(boolean enabled) {
		table.setEnabled(enabled);
	}

	private class ParameterLabel extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof Parameter))
				return null;
			Parameter parameter = (Parameter) element;
			switch (columnIndex) {
			case 0:
				return parameter.getName();
			case 1:
				return Double.toString(parameter.getValue());
			case 2:
				return UncertaintyLabel.get(parameter.getUncertainty());
			case 3:
				return parameter.getDescription();
			default:
				return null;
			}
		}
	}

	private class NameModifier extends TextCellModifier<Parameter> {

		@Override
		protected String getText(Parameter param) {
			return param.getName();
		}

		@Override
		protected void setText(Parameter param, String text) {
			if (text == null)
				return;
			if (Objects.equals(text, param.getName()))
				return;
			String name = text.trim();
			if (Parameter.isValidName(name))
				param.setName(name);
			else {
				Error.showBox(Messages.InvalidParameterName, "'" + name
						+ "' " + Messages.IsNotValidParameterName);
			}
		}
	}

	private class AmountModifier extends TextCellModifier<Parameter> {

		@Override
		protected String getText(Parameter param) {
			return Double.toString(param.getValue());
		}

		@Override
		protected void setText(Parameter param, String text) {
			try {
				double val = Double.parseDouble(text);
				param.setValue(val);
			} catch (Exception e) {
				Error.showBox(Messages.InvalidNumber, "'" + text
						+ "' " + Messages.IsNotValidNumber);
			}
		}
	}

	private class DescriptionModifier extends TextCellModifier<Parameter> {

		@Override
		protected String getText(Parameter param) {
			return param.getDescription();
		}

		@Override
		protected void setText(Parameter param, String text) {
			param.setDescription(text);
		}
	}

}
