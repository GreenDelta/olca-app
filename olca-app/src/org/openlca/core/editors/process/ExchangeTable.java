package org.openlca.core.editors.process;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.TableColumn;
import org.openlca.app.Messages;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Process;

final class ExchangeTable {

	// table columns

	static final int FLOW_COLUMN = 0;
	static final int CATEGORY_COLUM = 1;
	static final int PROPERTY_COLUMN = 2;
	static final int UNIT_COLUMN = 3;
	static final int AMOUNT_COLUMN = 4;
	static final int UNCERTAINTY_COLUMN = 5;
	static final int PROVIDER_COLUMN = 6;
	static final int AVOIDED_COLUMN = 6;
	static final int PEDIGREE_COLUMN = 7;

	// column properties and labels

	static final String AVOIDED_PRODUCT = Messages.Processes_AvoidedProduct
			+ "?";

	static final String CATEGORY = Messages.Common_Category;

	static final String FLOW = Messages.Common_Flow;

	static final String PROPERTY = Messages.Common_FlowProperty;

	static final String AMOUNT = Messages.Processes_ResultingAmount;

	static final String UNCERTAINTY = Messages.Common_Uncertainty;

	static final String UNIT = Messages.Processes_Unit;

	static final String PEDIGREE = Messages.Processes_PedigreeUncertainty;

	static final String PROVIDER = Messages.Processes_DefaultProvider;

	static final String[] INPUT_PROPERTIES = { FLOW, CATEGORY, PROPERTY, UNIT,
			AMOUNT, UNCERTAINTY, PROVIDER, PEDIGREE };

	static final String[] OUTPUT_PROPERTIES = { FLOW, CATEGORY, PROPERTY, UNIT,
			AMOUNT, UNCERTAINTY, AVOIDED_PRODUCT, PEDIGREE };

	private ExchangeTable() {
	}

	static void addSorting(TableViewer viewer, IDatabase database) {
		viewer.getTable().setSortDirection(SWT.DOWN);
		ExchangeSorter sorter = new ExchangeSorter();
		viewer.setSorter(sorter);
		ExchangeSortListener listener = new ExchangeSortListener(sorter, viewer);
		for (TableColumn c : viewer.getTable().getColumns()) {
			if (c.getText().equals(ExchangeTable.FLOW)) {
				c.setWidth(150);
				viewer.getTable().setSortColumn(c);
			}
			c.addSelectionListener(listener);
		}
	}

	static void setInput(Process process, TableViewer inputViewer,
			TableViewer outputViewer) {
		List<Exchange> inputs = new ArrayList<>();
		List<Exchange> outputs = new ArrayList<>();
		for (Exchange exchange : process.getExchanges()) {
			if (exchange.isInput() && exchange.isAvoidedProduct())
				outputs.add(exchange);
			else if (exchange.isInput())
				inputs.add(exchange);
			else
				outputs.add(exchange);
		}
		inputViewer.setInput(inputs);
		outputViewer.setInput(outputs);
	}

	static void addInputEditors(TableViewer viewer, IDatabase database) {
		CellEditor[] editors = initEditors(viewer);
		editors[PROVIDER_COLUMN] = createComboEditor(viewer);
		viewer.setCellEditors(editors);
		viewer.setCellModifier(new ExchangeCellModifier(viewer, database, true));
	}

	static void addOutputEditors(TableViewer viewer, IDatabase database) {
		CellEditor[] editors = initEditors(viewer);
		editors[AVOIDED_COLUMN] = new CheckboxCellEditor(viewer.getTable());
		viewer.setCellEditors(editors);
		viewer.setCellModifier(new ExchangeCellModifier(viewer, database, false));
	}

	private static CellEditor[] initEditors(TableViewer viewer) {
		CellEditor[] editors = new CellEditor[8];
		editors[PROPERTY_COLUMN] = createComboEditor(viewer);
		editors[UNIT_COLUMN] = createComboEditor(viewer);
		editors[AMOUNT_COLUMN] = new TextCellEditor(viewer.getTable());
		editors[UNCERTAINTY_COLUMN] = createComboEditor(viewer);
		editors[PEDIGREE_COLUMN] = new PedigreeCellEditor(viewer.getTable());
		return editors;
	}

	private static ComboBoxCellEditor createComboEditor(TableViewer viewer) {
		return new ComboBoxCellEditor(viewer.getTable(), new String[0],
				SWT.READ_ONLY);
	}

}
