package org.openlca.app.editors.processes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.TableClipboard;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.model.AllocationFactor;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Process;
import org.openlca.io.CategoryPath;
import org.openlca.util.Strings;

/**
 * A table for the display and editing of the causal allocation factors of a
 * process. The output products are displayed in columns.
 */
class CausalFactorTable {

	private ProcessEditor editor;
	private Column[] columns;
	private TableViewer viewer;

	public CausalFactorTable(ProcessEditor editor) {
		this.editor = editor;
		initColumns();
	}

	private Process process() {
		return editor.getModel();
	}

	public void refresh() {
		List<Exchange> products = Processes.getOutputProducts(process());
		List<Exchange> newProducts = new ArrayList<>(products);
		List<Integer> removalIndices = new ArrayList<>();
		for (int i = 0; i < columns.length; i++) {
			Exchange product = columns[i].product;
			if (products.contains(product))
				newProducts.remove(product);
			else
				removalIndices.add(i);
		}
		for (int col : removalIndices)
			removeColumn(col);
		for (Exchange product : newProducts)
			addColumn(product);
		viewer.setInput(Processes.getNonOutputProducts(process()));
		createModifySupport();
	}

	private void removeColumn(int col) {
		Column[] newColumns = new Column[columns.length - 1];
		System.arraycopy(columns, 0, newColumns, 0, col);
		if ((col + 1) < columns.length)
			System.arraycopy(columns, col + 1, newColumns, col,
					newColumns.length - col);
		columns = newColumns;
		Table table = viewer.getTable();
		table.getColumn(col + 4).dispose();
	}

	private void addColumn(Exchange product) {
		Column newColumn = new Column(product);
		Table table = viewer.getTable();
		TableColumn tableColumn = new TableColumn(table, SWT.VIRTUAL);
		tableColumn.setText(newColumn.getTitle());
		tableColumn.setWidth(150);
		Column[] newColumns = new Column[columns.length + 1];
		System.arraycopy(columns, 0, newColumns, 0, columns.length);
		newColumns[columns.length] = newColumn;
		columns = newColumns;
	}

	private void initColumns() {
		List<Exchange> products = Processes.getOutputProducts(process());
		columns = new Column[products.size()];
		for (int i = 0; i < columns.length; i++)
			columns[i] = new Column(products.get(i));
		Arrays.sort(columns);
	}

	public void render(Section section, FormToolkit toolkit) {
		Composite composite = UI.sectionClient(section, toolkit);
		UI.gridLayout(composite, 1);
		String[] columnTitles = getColumnTitles();
		viewer = Tables.createViewer(composite, columnTitles);
		viewer.setLabelProvider(new FactorLabel());
		Action copy = TableClipboard.onCopy(viewer);
		Actions.bind(viewer, copy);
		Tables.bindColumnWidths(viewer, 0.2, 0.1, 0.1, 0.1);
		createModifySupport();
		Table table = viewer.getTable();
		for (int i = 0; i < table.getColumnCount(); i++) {
			if (i < 4)
				continue;
			TableColumn column = table.getColumn(i);
			column.setWidth(80);
			column.setToolTipText(columnTitles[i]);
		}
	}

	void setInitialInput() {
		viewer.setInput(Processes.getNonOutputProducts(process()));
	}

	private void createModifySupport() {
		String[] keys = getColumnTitles();
		for (int i = 0; i < columns.length; i++)
			keys[i + 4] = columns[i].getKey();
		viewer.setColumnProperties(keys);
		ModifySupport<Exchange> modifySupport = new ModifySupport<>(viewer);
		for (int i = 0; i < columns.length; i++)
			modifySupport.bind(keys[i + 4], new ValueModifier(
					columns[i].product));
	}

	private String[] getColumnTitles() {
		String[] titles = new String[columns.length + 4];
		titles[0] = Messages.Flow;
		titles[1] = Messages.Direction;
		titles[2] = Messages.Category;
		titles[3] = Messages.Amount;
		for (int i = 0; i < columns.length; i++)
			titles[i + 4] = columns[i].getTitle();
		return titles;
	}

	private AllocationFactor getFactor(Exchange product, Exchange exchange) {
		AllocationFactor factor = null;
		for (AllocationFactor f : process().getAllocationFactors()) {
			if (f.getAllocationType() != AllocationMethod.CAUSAL)
				continue;
			if (product.getFlow().getId() == f.getProductId()
					&& Objects.equals(f.getExchange(), exchange)) {
				factor = f;
				break;
			}
		}
		return factor;
	}

	private class FactorLabel extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int col) {
			if (col != 0)
				return null;
			if (!(element instanceof Exchange))
				return null;
			Exchange exchange = (Exchange) element;
			if (exchange.getFlow() == null)
				return null;
			return Images.getIcon(exchange.getFlow().getFlowType());
		}

		@Override
		public String getColumnText(Object element, int col) {
			if (!(element instanceof Exchange))
				return null;
			Exchange exchange = (Exchange) element;
			if (exchange.getFlow() == null || exchange.getUnit() == null)
				return null;
			switch (col) {
			case 0:
				return Labels.getDisplayName(exchange.getFlow());
			case 1:
				return exchange.isInput() ? Messages.Input : Messages.Output;
			case 2:
				return CategoryPath.getShort(exchange.getFlow().getCategory());
			case 3:
				return Numbers.format(exchange.getAmountValue()) + " "
						+ exchange.getUnit().getName();
			default:
				return getFactorLabel(exchange, col);
			}
		}

		private String getFactorLabel(Exchange exchange, int col) {
			int idx = col - 4;
			if (idx < 0 || idx > (columns.length - 1))
				return null;
			Column column = columns[idx];
			AllocationFactor factor = getFactor(column.getProduct(), exchange);
			if (factor == null)
				return Double.toString(1.0);
			else
				return Double.toString(factor.getValue());
		}
	}

	private class Column implements Comparable<Column> {

		private Exchange product;
		private String key;

		public Column(Exchange product) {
			this.product = product;
			key = UUID.randomUUID().toString();
		}

		public Exchange getProduct() {
			return product;
		}

		public String getKey() {
			return key;
		}

		public String getTitle() {
			if (product == null || product.getFlow() == null)
				return "";
			return Labels.getDisplayName(product.getFlow());
		}

		@Override
		public int compareTo(Column o) {
			return Strings.compare(this.getTitle(), o.getTitle());
		}
	}

	private class ValueModifier extends TextCellModifier<Exchange> {

		private Exchange product;

		public ValueModifier(Exchange product) {
			this.product = product;
		}

		@Override
		protected String getText(Exchange exchange) {
			AllocationFactor factor = getFactor(product, exchange);
			if (factor == null)
				return Double.toString(1);
			else
				return Double.toString(factor.getValue());
		}

		@Override
		protected void setText(Exchange exchange, String text) {
			Double val = AllocationPage.parseFactor(text);
			if (val == null)
				return;
			AllocationFactor factor = getFactor(product, exchange);
			if (factor == null) {
				factor = new AllocationFactor();
				factor.setAllocationType(AllocationMethod.CAUSAL);
				factor.setExchange(exchange);
				factor.setProductId(product.getFlow().getId());
				process().getAllocationFactors().add(factor);
			}
			factor.setValue(val);
			editor.setDirty(true);
		}
	}

}
