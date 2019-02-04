package org.openlca.app.editors.processes.allocation;

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
import org.openlca.app.M;
import org.openlca.app.editors.comments.CommentDialogModifier;
import org.openlca.app.editors.comments.CommentPaths;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.TableClipboard;
import org.openlca.app.util.tables.Tables;
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
		List<Exchange> products = Util.getProviderFlows(process());
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
		viewer.setInput(Util.getNonProviderFlows(process()));
		createModifySupport();
		viewer.refresh(true);
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
		if (editor.hasAnyComment("allocationFactors"))
			table.getColumn(col + 5).dispose();
	}

	private void addColumn(Exchange product) {
		Column newColumn = new Column(product);
		Table table = viewer.getTable();
		TableColumn tableColumn = new TableColumn(table, SWT.VIRTUAL);
		tableColumn.setText(newColumn.getTitle());
		tableColumn.setToolTipText(newColumn.getTitle());
		tableColumn.setWidth(80);
		Column[] newColumns = new Column[columns.length + 1];
		System.arraycopy(columns, 0, newColumns, 0, columns.length);
		newColumns[columns.length] = newColumn;
		columns = newColumns;
		if (editor.hasAnyComment("allocationFactors")) {
			new TableColumn(table, SWT.VIRTUAL).setWidth(24);
		}
	}

	private void initColumns() {
		List<Exchange> pFlows = Util.getProviderFlows(process());
		columns = new Column[pFlows.size()];
		for (int i = 0; i < columns.length; i++) {
			columns[i] = new Column(pFlows.get(i));
		}
		Arrays.sort(columns);
	}

	public void render(Section section, FormToolkit toolkit) {
		Composite composite = UI.sectionClient(section, toolkit, 1);
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
			if (!editor.hasAnyComment("allocationFactors") || i % 2 == 0) {
				column.setWidth(80);
				column.setToolTipText(columnTitles[i]);
			} else {
				column.setWidth(24);
			}
		}
		for (int i = 3; i < table.getColumnCount(); i++) {
			viewer.getTable().getColumns()[i].setAlignment(SWT.RIGHT);
		}
	}

	void setInitialInput() {
		viewer.setInput(Util.getNonProviderFlows(process()));
	}

	private void createModifySupport() {
		String[] keys = getColumnTitles();
		boolean showComments = editor.hasAnyComment("allocationFactors");
		for (int i = 0; i < columns.length; i++) {
			int index = showComments ? 2 * i : i;
			keys[index + 4] = columns[i].key;
			if (showComments) {
				keys[index + 5] = columns[i].key + "-comment";
			}
		}
		viewer.setColumnProperties(keys);
		ModifySupport<Exchange> modifySupport = new ModifySupport<>(viewer);
		for (int i = 0; i < columns.length; i++) {
			int index = showComments ? 2 * i : i;
			modifySupport.bind(keys[index + 4], new ValueModifier(columns[i].product));
			if (showComments) {
				Exchange product = columns[i].product;
				modifySupport.bind(keys[index + 5], new CommentDialogModifier<Exchange>(editor.getComments(),
						(e) -> CommentPaths.get(getFactor(product, e), product, e)));
			}
		}
	}

	private String[] getColumnTitles() {
		boolean showComments = editor.hasAnyComment("allocationFactors");
		int cols = showComments ? columns.length * 2 : columns.length;
		String[] titles = new String[cols + 4];
		titles[0] = M.Flow;
		titles[1] = M.Direction;
		titles[2] = M.Category;
		titles[3] = M.Amount;
		for (int i = 0; i < columns.length; i++) {
			int index = showComments ? 2 * i : i;
			titles[index + 4] = columns[i].getTitle();
			if (showComments) {
				titles[index + 5] = "";
			}
		}
		return titles;
	}

	private Exchange getProduct(int col) {
		int idx = (col - 4);
		if (editor.hasAnyComment("allocationFactors")) {
			idx /= 2;
		}
		if (idx < 0 || idx > (columns.length - 1))
			return null;
		return columns[idx].product;
	}

	private AllocationFactor getFactor(Exchange product, Exchange exchange) {
		for (AllocationFactor factor : process().allocationFactors) {
			if (factor.method != AllocationMethod.CAUSAL)
				continue;
			if (product.flow.id != factor.productId)
				continue;
			if (!Objects.equals(factor.exchange, exchange))
				continue;
			return factor;
		}
		return null;
	}

	private class FactorLabel extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int col) {
			if (!(element instanceof Exchange))
				return null;
			Exchange exchange = (Exchange) element;
			if (exchange.flow == null)
				return null;
			if (col == 0)
				return Images.get(exchange.flow);
			if (col > 3 && col % 2 == 1 && editor.hasAnyComment("allocationFactors")) {
				Exchange product = getProduct(col);
				AllocationFactor factor = getFactor(product, exchange);
				if (factor == null)
					return null;
				return Images.get(editor.getComments(), CommentPaths.get(factor, product, exchange));
			}
			return null;
		}

		@Override
		public String getColumnText(Object element, int col) {
			if (!(element instanceof Exchange))
				return null;
			Exchange exchange = (Exchange) element;
			if (exchange.flow == null || exchange.unit == null)
				return null;
			switch (col) {
			case 0:
				return Labels.getDisplayName(exchange.flow);
			case 1:
				return exchange.isInput ? M.Input : M.Output;
			case 2:
				return CategoryPath.getShort(exchange.flow.category);
			case 3:
				return Numbers.format(exchange.amount) + " "
						+ exchange.unit.name;
			default:
				if (col % 2 == 0 || !editor.hasAnyComment("allocationFactors"))
					return getFactorLabel(exchange, col);
				return null;
			}
		}

		private String getFactorLabel(Exchange exchange, int col) {
			AllocationFactor factor = getFactor(getProduct(col), exchange);
			if (factor == null)
				return Double.toString(1.0);
			return Double.toString(factor.value);
		}
	}

	private class Column implements Comparable<Column> {

		private Exchange product;
		private String key;

		public Column(Exchange product) {
			this.product = product;
			key = UUID.randomUUID().toString();
		}

		public String getTitle() {
			if (product == null || product.flow == null)
				return "";
			return Labels.getDisplayName(product.flow);
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
			return Double.toString(factor.value);
		}

		@Override
		protected void setText(Exchange exchange, String text) {
			Double val = AllocationPage.parseFactor(text);
			if (val == null)
				return;
			AllocationFactor factor = getFactor(product, exchange);
			if (factor == null) {
				factor = new AllocationFactor();
				factor.method = AllocationMethod.CAUSAL;
				factor.exchange = exchange;
				factor.productId = product.flow.id;
				process().allocationFactors.add(factor);
			}
			factor.value = val;
			editor.setDirty(true);
		}
	}

}
