package org.openlca.app.editors.processes.allocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
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
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.viewers.tables.modify.ModifySupport;
import org.openlca.app.viewers.tables.modify.TextCellModifier;
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

	private final ProcessEditor editor;
	private final AllocationPage page;
	private final boolean withComments;
	private final List<Column> columns = new ArrayList<>();

	private TableViewer viewer;
	private ModifySupport<Exchange> modifier;

	public CausalFactorTable(AllocationPage page) {
		this.page = page;
		this.editor = page.editor;
		this.withComments = editor.hasAnyComment("allocationFactors");
		initColumns();
	}

	void render(Section section, FormToolkit tk) {
		var comp = UI.sectionClient(section, tk, 1);
		viewer = Tables.createViewer(comp,
			M.Flow, M.Direction, M.Category, M.Amount);
		viewer.setLabelProvider(new FactorLabel());
		var copy = TableClipboard.onCopySelected(viewer);
		Actions.bind(viewer, copy);
		Tables.bindColumnWidths(viewer, 0.2, 0.1, 0.1, 0.1);
		modifier = new ModifySupport<>(viewer);
		refresh();
	}

	private Process process() {
		return editor.getModel();
	}

	void refresh() {
		if (viewer == null)
			return;
		var products = Util.getProviderFlows(process());



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
		if (withComments) {
			table.getColumn(col + 5).dispose();
		}
	}

	private void addColumn(Exchange product) {
		Column newColumn = new Column(product);
		Table table = viewer.getTable();
		var col = new TableColumn(table, SWT.VIRTUAL);
		col.setText(newColumn.title());
		col.setToolTipText(newColumn.title());
		col.setWidth(80);
		Column[] newColumns = new Column[columns.length + 1];
		System.arraycopy(columns, 0, newColumns, 0, columns.length);
		newColumns[columns.length] = newColumn;
		columns = newColumns;
		if (withComments) {
			new TableColumn(table, SWT.VIRTUAL).setWidth(24);
		}
	}

	private void initColumns() {
		var products = Util.getProviderFlows(process());
		columns = new Column[products.size()];
		for (int i = 0; i < columns.length; i++) {
			columns[i] = new Column(products.get(i));
		}
		Arrays.sort(columns);
	}

	void setInitialInput() {
		viewer.setInput(Util.getNonProviderFlows(process()));
	}

	private void createModifySupport() {
		if (!editor.isEditable())
			return;
		String[] keys = getColumnTitles();
		for (int i = 0; i < columns.length; i++) {
			int index = withComments ? 2 * i : i;
			keys[index + 4] = columns[i].key;
			if (withComments) {
				keys[index + 5] = columns[i].key + "-comment";
			}
		}
		viewer.setColumnProperties(keys);


		for (int i = 0; i < columns.length; i++) {
			int index = withComments ? 2 * i : i;
			modifier.bind(keys[index + 4], columns[i]);

			if (withComments) {
				var product = columns[i].product;
				modifier.bind(keys[index + 5], new CommentDialogModifier<>(editor.getComments(),
					(e) -> CommentPaths.get(getFactor(product, e), product, e)));
			}
		}
	}

	private class FactorLabel extends LabelProvider implements
		ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int col) {
			if (!(element instanceof Exchange exchange))
				return null;
			if (exchange.flow == null)
				return null;
			if (col == 0)
				return Images.get(exchange.flow);

			// comment images
			if (col > 4) {
				for (var c : columns) {
					if (c.commentIdx == col) {
						var factor = c.factorOf(exchange);
						if (factor == null)
							return null;
						return Images.get(
							editor.getComments(),
							c.commentPathOf(exchange));
					}
				}
			}
			return null;
		}

		@Override
		public String getColumnText(Object element, int col) {
			if (!(element instanceof Exchange exchange))
				return null;
			if (exchange.flow == null || exchange.unit == null)
				return null;
			return switch (col) {
				case 0 -> Labels.name(exchange.flow);
				case 1 -> exchange.isInput ? M.Input : M.Output;
				case 2 -> CategoryPath.getShort(exchange.flow.category);
				case 3 -> Numbers.format(exchange.amount) + " "
					+ exchange.unit.name;
				default -> {
					for (var c : columns) {
						if (col == c.commentIdx)
							yield null;
						if (col == c.idx) {
							var f = c.factorOf(exchange);
							if (f == null)
								yield "";
							yield Strings.nullOrEmpty(f.formula)
								? Double.toString(f.value)
								: f.formula + " = " + f.value;
						}
					}
					yield null;
				}
			};
		}
	}

	private class Column extends TextCellModifier<Exchange> {

		private final Exchange product;
		private final String key;
		private final int idx;
		private final int commentIdx;

		public Column(Exchange product) {
			this.product = Objects.requireNonNull(product);
			key = UUID.randomUUID().toString();

			// create the table column; if comments are active
			// an additional comment column is created on the
			// right side of the column
			var table = viewer.getTable();
			var col = new TableColumn(table, SWT.VIRTUAL);
			col.setText(title());
			col.setToolTipText(title());
			col.setWidth(80);
			idx = table.indexOf(col);
			if (withComments) {
				var commentCol = new TableColumn(table, SWT.VIRTUAL);
				commentCol.setWidth(24);
				commentIdx = table.indexOf(commentCol);
			} else {
				commentIdx = -1;
			}
		}

		public String title() {
			return Labels.name(product.flow);
		}

		void dispose() {
			var table = viewer.getTable();
			var col = table.getColumn(idx);
			if (col != null) {
				col.dispose();
			}
			if (commentIdx >= 0) {
				var commentCol = table.getColumn(commentIdx);
				if (commentCol != null) {
					commentCol.dispose();
				}
			}
		}

		AllocationFactor factorOf(Exchange exchange) {
			if (exchange == null)
				return null;
			for (var factor : process().allocationFactors) {
				if (factor.method != AllocationMethod.CAUSAL
					|| product.flow.id != factor.productId)
					continue;
				if (Objects.equals(factor.exchange, exchange))
					return factor;
			}
			return null;
		}

		@Override
		protected String getText(Exchange exchange) {
			var factor = factorOf(exchange);
			if (factor == null)
				return "";
			return Strings.nullOrEmpty(factor.formula)
				? Double.toString(factor.value)
				: factor.formula;
		}

		@Override
		protected void setText(Exchange exchange, String text) {
			var factor = factorOf(exchange);
			boolean isNew = factor == null;
			if (isNew) {
				factor = new AllocationFactor();
				factor.method = AllocationMethod.CAUSAL;
				factor.exchange = exchange;
				factor.productId = product.flow.id;
			}
			if (page.update(factor, text)) {
				if (isNew) {
					process().allocationFactors.add(factor);
				}
				editor.setDirty(true);
			}
		}

		String commentPathOf(Exchange exchange) {
			var factor = factorOf(exchange);
			return factor != null
				? CommentPaths.get(factor, product, exchange)
				: null;
		}

	}

}
