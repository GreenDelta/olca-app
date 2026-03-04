package org.openlca.app.editors.sd.editor.graph.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.sd.model.Dimension;
import org.openlca.sd.model.Id;
import org.openlca.sd.model.Tensor;
import org.openlca.sd.model.cells.BoolCell;
import org.openlca.sd.model.cells.Cell;
import org.openlca.sd.model.cells.EqnCell;
import org.openlca.sd.model.cells.NonNegativeCell;
import org.openlca.sd.model.cells.NumCell;
import org.openlca.sd.model.cells.TensorCell;
import org.openlca.sd.model.cells.TensorEqnCell;

/// Panel for editing tensor (array) cells: TensorCell, TensorEqnCell.
/// Shows an equation text box and a table for array values.
/// Supports 1D and 2D arrays. The row and column dimensions can be
/// configured via a context menu.
class TensorPanel {

	final Composite composite;
	private final Text equationText;
	private final TableViewer table;

	/// Current dimension configuration. For a 1D array only rowDim is set.
	private Dimension rowDim;
	private Dimension colDim;

	/// Internal data model: list of rows; each row is an array of cell strings.
	/// For 1D: each row has 1 value column. For 2D: columns match colDim.
	private final List<String[]> data = new ArrayList<>();

	TensorPanel(Composite parent, FormToolkit tk) {
		composite = UI.composite(parent, tk);
		UI.gridLayout(composite, 2);
		UI.gridData(composite, true, true);

		equationText = UI.labeledMultiText(composite, tk, "Equation", 80);

		var tableLabel = UI.label(composite, tk, "Array values");
		var gd = UI.gridData(tableLabel, true, false);
		gd.horizontalSpan = 2;

		table = new TableViewer(composite,
			SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		var tableGd = UI.gridData(table.getControl(), true, true);
		tableGd.horizontalSpan = 2;
		tableGd.heightHint = 200;
		table.getTable().setHeaderVisible(true);
		table.getTable().setLinesVisible(true);
		table.setContentProvider(ArrayContentProvider.getInstance());

		createContextMenu();
	}

	private void createContextMenu() {
		var menu = new Menu(table.getTable());

		var setRowDimItem = new MenuItem(menu, SWT.PUSH);
		setRowDimItem.setText("Set row dimension...");
		Controls.onSelect(setRowDimItem, e -> {
			var input = promptDimension("Row dimension",
				"Enter dimension as: name: elem1, elem2, ...");
			if (input != null) {
				rowDim = input;
				colDim = null;
				rebuildTable();
			}
		});

		var setColDimItem = new MenuItem(menu, SWT.PUSH);
		setColDimItem.setText("Set column dimension...");
		Controls.onSelect(setColDimItem, e -> {
			if (rowDim == null) {
				return; // need a row dimension first
			}
			var input = promptDimension("Column dimension",
				"Enter dimension as: name: elem1, elem2, ...");
			if (input != null) {
				colDim = input;
				rebuildTable();
			}
		});

		new MenuItem(menu, SWT.SEPARATOR);

		var addRowItem = new MenuItem(menu, SWT.PUSH);
		addRowItem.setText("Add row");
		Controls.onSelect(addRowItem, e -> {
			int cols = colDim != null ? colDim.size() : 1;
			var row = new String[cols];
			for (int i = 0; i < cols; i++) {
				row[i] = "0";
			}
			data.add(row);
			table.setInput(data);
		});

		var removeItem = new MenuItem(menu, SWT.PUSH);
		removeItem.setText("Remove selected rows");
		Controls.onSelect(removeItem, e -> {
			var sel = table.getTable().getSelectionIndices();
			for (int i = sel.length - 1; i >= 0; i--) {
				if (sel[i] >= 0 && sel[i] < data.size()) {
					data.remove(sel[i]);
				}
			}
			table.setInput(data);
		});

		table.getTable().setMenu(menu);
	}

	private Dimension promptDimension(String title, String message) {
		var dialog = new org.eclipse.jface.dialogs.InputDialog(
			composite.getShell(), title, message, "", null);
		if (dialog.open() != org.eclipse.jface.window.Window.OK)
			return null;
		var text = dialog.getValue();
		if (text == null || text.isBlank())
			return null;

		// parse "name: elem1, elem2, elem3"
		var parts = text.split(":", 2);
		var name = parts[0].trim();
		if (name.isEmpty())
			return null;
		if (parts.length < 2)
			return Dimension.of(name);

		var elems = parts[1].split(",");
		var trimmed = new String[elems.length];
		for (int i = 0; i < elems.length; i++) {
			trimmed[i] = elems[i].trim();
		}
		return Dimension.of(name, trimmed);
	}

	/// Rebuilds table columns and data when dimensions change.
	private void rebuildTable() {
		// remove all old columns
		for (var col : table.getTable().getColumns()) {
			col.dispose();
		}

		int colCount = colDim != null ? colDim.size() : 1;

		// row label column
		var rowCol = new TableViewerColumn(table, SWT.NONE);
		rowCol.getColumn().setText(
			rowDim != null ? rowDim.name().label() : "Index");
		rowCol.getColumn().setWidth(100);
		rowCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				int idx = data.indexOf(element);
				if (rowDim != null && idx >= 0
					&& idx < rowDim.elements().size()) {
					return rowDim.elements().get(idx).label();
				}
				return Integer.toString(idx);
			}
		});

		// value columns
		for (int c = 0; c < colCount; c++) {
			final int ci = c;
			var vc = new TableViewerColumn(table, SWT.NONE);
			vc.getColumn().setText(
				colDim != null ? colDim.elements().get(c).label()
					: "Value");
			vc.getColumn().setWidth(120);
			vc.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					if (element instanceof String[] row && ci < row.length) {
						return row[ci];
					}
					return "";
				}
			});
			vc.setEditingSupport(new ArrayEditingSupport(table, ci));
		}

		// rebuild data rows
		int rowCount = rowDim != null ? rowDim.size() : data.size();
		var oldData = new ArrayList<>(data);
		data.clear();
		for (int r = 0; r < rowCount; r++) {
			var row = new String[colCount];
			if (r < oldData.size()) {
				var old = oldData.get(r);
				for (int c = 0; c < colCount; c++) {
					row[c] = c < old.length ? old[c] : "0";
				}
			} else {
				for (int c = 0; c < colCount; c++) {
					row[c] = "0";
				}
			}
			data.add(row);
		}
		table.setInput(data);
	}

	void setInput(Cell cell) {
		var unwrapped = cell instanceof NonNegativeCell(Cell inner)
			? inner
			: cell;

		data.clear();
		rowDim = null;
		colDim = null;

		switch (unwrapped) {
			case TensorEqnCell(Cell eqn, Tensor tensor) -> {
				equationText.setText(eqnToText(eqn));
				loadTensor(tensor);
			}
			case TensorCell(Tensor tensor) -> {
				equationText.setText("");
				loadTensor(tensor);
			}
			default -> equationText.setText("");
		}
		rebuildTable();
	}

	private String eqnToText(Cell eqn) {
		return switch (eqn) {
			case EqnCell(String s) -> s != null ? s : "";
			case NumCell(double num) -> Double.toString(num);
			case BoolCell(boolean b) -> Boolean.toString(b);
			case null, default -> "";
		};
	}

	private void loadTensor(Tensor tensor) {
		if (tensor == null)
			return;
		var dims = tensor.dimensions();
		if (dims.isEmpty())
			return;

		rowDim = dims.getFirst();
		if (dims.size() > 1) {
			colDim = dims.get(1);
		}

		int colCount = colDim != null ? colDim.size() : 1;
		for (int r = 0; r < tensor.size(); r++) {
			var row = new String[colCount];
			var rowCell = tensor.get(r);
			if (colDim != null && rowCell instanceof TensorCell(Tensor sub)) {
				for (int c = 0; c < colCount && c < sub.size(); c++) {
					row[c] = cellToString(sub.get(c));
				}
			} else {
				row[0] = cellToString(rowCell);
			}
			data.add(row);
		}
	}

	private String cellToString(Cell cell) {
		return switch (cell) {
			case NumCell(double num) -> Double.toString(num);
			case EqnCell(String eqn) -> eqn != null ? eqn : "0";
			case BoolCell(boolean b) -> Boolean.toString(b);
			case NonNegativeCell(Cell inner) -> cellToString(inner);
			case null, default -> "0";
		};
	}

	Cell getCell() {
		var tensor = buildTensor();
		if (tensor == null)
			return Cell.empty();
		var eqn = equationText.getText().trim();
		if (eqn.isEmpty()) {
			return Cell.of(tensor);
		}
		return new TensorEqnCell(Cell.of(eqn), tensor);
	}

	private Tensor buildTensor() {
		if (rowDim == null) {
			// fallback: create a dimension from the row count
			if (data.isEmpty())
				return null;
			var elems = new String[data.size()];
			for (int i = 0; i < elems.length; i++) {
				elems[i] = Integer.toString(i);
			}
			rowDim = Dimension.of("dim", elems);
		}

		Tensor tensor;
		if (colDim != null) {
			tensor = Tensor.of(rowDim, colDim);
		} else {
			tensor = Tensor.of(rowDim);
		}

		for (int r = 0; r < data.size() && r < tensor.size(); r++) {
			var row = data.get(r);
			if (colDim != null) {
				var rowCell = tensor.get(r);
				if (rowCell instanceof TensorCell(Tensor sub)) {
					for (int c = 0; c < row.length && c < sub.size(); c++) {
						sub.set(c, Cell.of(row[c]));
					}
				}
			} else {
				tensor.set(r, Cell.of(row[0]));
			}
		}
		return tensor;
	}

	Text equationText() {
		return equationText;
	}

	private class ArrayEditingSupport extends EditingSupport {

		private final int colIndex;

		ArrayEditingSupport(TableViewer viewer, int colIndex) {
			super(viewer);
			this.colIndex = colIndex;
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return new TextCellEditor(table.getTable());
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}

		@Override
		protected Object getValue(Object element) {
			if (element instanceof String[] row && colIndex < row.length) {
				return row[colIndex];
			}
			return "0";
		}

		@Override
		protected void setValue(Object element, Object value) {
			if (element instanceof String[] row && colIndex < row.length) {
				row[colIndex] = value != null ? value.toString() : "0";
				table.refresh();
			}
		}
	}
}
