package org.openlca.app.editors.processes.allocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
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
import org.openlca.app.util.Colors;
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
import org.openlca.util.AllocationUtils;
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
	private SumColumn sumColumn;

	private TableViewer viewer;
	private ModifySupport<Exchange> modifier;

	public CausalFactorTable(AllocationPage page) {
		this.page = page;
		this.editor = page.editor;
		this.withComments = editor.hasAnyComment("allocationFactors");
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
		var products = AllocationUtils.getProviderFlows(process());

		if (products.size() < 2) {
			disposeColumns();
			viewer.setInput(Collections.emptyList());
			return;
		}

		// first test if we need to rebuild the columns
		var needRebuild = products.size() != columns.size();
		if (!needRebuild) {
			for (var col : columns) {
				if (!products.contains(col.product)) {
					needRebuild = true;
					break;
				}
			}
		}

		// rebuild the columns if required; order is important!
		if (needRebuild) {

			disposeColumns();

			// create the new columns
			products.stream()
				.sorted(Comparator.comparing(p -> Labels.name(p.flow)))
				.map(Column::new)
				.forEach(columns::add);
			sumColumn = SumColumn.addTo(viewer);

			// update the viewer properties
			var table = viewer.getTable();
			var props = new String[table.getColumnCount()];
			for (int i = 0; i < props.length; i++) {
				var col = table.getColumn(i);
				props[i] = col.getText();
			}
			for (var col : columns) {
				props[col.idx()] = col.key;
				if (withComments && col.commentIdx() > 0) {
					props[col.commentIdx()] = col.key + "_comment";
				}
			}
			viewer.setColumnProperties(props);

			// bind the new columns to the modifier
			for (var column : columns) {
				modifier.bind(column.key, column);
				if (withComments) {
					modifier.bind(column.key + "_comment",
						new CommentDialogModifier<>(
							editor.getComments(), column::commentPathOf));
				}
			}
		}

		// set the table
		viewer.setInput(AllocationUtils.getNonProviderFlows(process()));
	}

	private void disposeColumns() {
		if (sumColumn != null) {
			sumColumn.dispose();
			sumColumn = null;
		}
		if (columns.isEmpty())
			return;
		for (var old : columns) {
			modifier.unbind(old.key);
			if (withComments) {
				modifier.unbind(old.key + "_comment");
			}
		}
		columns.forEach(Column::dispose);
		columns.clear();
	}

	private class FactorLabel extends LabelProvider implements
		ITableLabelProvider, ITableFontProvider, ITableColorProvider {

		@Override
		public Font getFont(Object obj, int col) {
			return sumColumn != null && sumColumn.hasIndex(col)
				? UI.boldFont()
				: null;
		}

		@Override
		public Color getBackground(Object obj, int col) {
			return null;
		}

		@Override
		public Color getForeground(Object obj, int col) {
			if (sumColumn == null
				|| !sumColumn.hasIndex(col)
				|| !(obj instanceof Exchange exchange))
				return null;
			var sum = sumColumn.sumOf(exchange, process());
			return Math.abs(sum - 1) > 1e-4
				? Colors.red()
				: null;
		}

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof Exchange exchange))
				return null;
			if (exchange.flow == null)
				return null;
			if (col == 0)
				return Images.get(exchange.flow);

			// comment images
			if (withComments && col > 4) {
				for (var c : columns) {
					if (c.commentIdx() == col) {
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

					// factors of columns
					for (var c : columns) {
						if (withComments && col == c.commentIdx())
							yield null;
						if (col == c.idx()) {
							var f = c.factorOf(exchange);
							if (f == null)
								yield "";
							yield Strings.nullOrEmpty(f.formula)
								? Double.toString(f.value)
								: f.formula + " = " + f.value;
						}
					}

					// sum column
					if (sumColumn != null && sumColumn.hasIndex(col)) {
						yield Numbers.format(sumColumn.sumOf(exchange, process()));
					}

					yield null;
				}
			};
		}

	}

	private class Column extends TextCellModifier<Exchange> {

		private final Exchange product;
		private final String key;
		private final TableColumn _col;
		private final TableColumn _commentCol;

		public Column(Exchange product) {
			this.product = Objects.requireNonNull(product);
			key = UUID.randomUUID().toString();

			// create the table column; if comments are active
			// an additional comment column is created on the
			// right side of the column
			var table = viewer.getTable();
			_col = new TableColumn(table, SWT.VIRTUAL);
			_col.setText(title());
			_col.setToolTipText(title());
			_col.setWidth(80);
			_col.setAlignment(SWT.CENTER);
			if (withComments) {
				_commentCol = new TableColumn(table, SWT.VIRTUAL);
				_commentCol.setWidth(24);
			} else {
				_commentCol = null;
			}
		}

		int idx() {
			return viewer.getTable().indexOf(_col);
		}

		int commentIdx() {
			return _commentCol != null
				? viewer.getTable().indexOf(_commentCol)
				: -1;
		}

		String title() {
			return Labels.name(product.flow);
		}

		void dispose() {
			_col.dispose();
			if (_commentCol != null) {
				_commentCol.dispose();
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

	private record SumColumn(Table table, TableColumn col) {

		static SumColumn addTo(TableViewer viewer) {
			var table = viewer.getTable();
			var col = new TableColumn(table, SWT.VIRTUAL);
			col.setText("\u03a3");
			col.setToolTipText("Sum of allocation factors");
			col.setWidth(80);
			col.setAlignment(SWT.CENTER);
			return new SumColumn(table, col);
		}

		void dispose() {
			col.dispose();
		}

		boolean hasIndex(int idx) {
			return idx == table.indexOf(col);
		}

		double sumOf(Exchange exchange, Process process) {
			if (exchange == null || process == null)
				return 0;
			return process.allocationFactors.stream()
				.filter(f -> f.method == AllocationMethod.CAUSAL
					&& Objects.equals(exchange, f.exchange))
				.mapToDouble(f -> f.value)
				.sum();
		}
	}
}
