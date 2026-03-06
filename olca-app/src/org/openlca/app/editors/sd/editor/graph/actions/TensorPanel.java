package org.openlca.app.editors.sd.editor.graph.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.UI;
import org.openlca.sd.model.Dimension;
import org.openlca.sd.model.Tensor;
import org.openlca.sd.model.cells.BoolCell;
import org.openlca.sd.model.cells.Cell;
import org.openlca.sd.model.cells.EqnCell;
import org.openlca.sd.model.cells.NonNegativeCell;
import org.openlca.sd.model.cells.NumCell;
import org.openlca.sd.model.cells.TensorCell;
import org.openlca.sd.model.cells.TensorEqnCell;

/// Read-only panel for displaying tensor (array) cells: TensorCell,
/// TensorEqnCell. Shows an equation text box and a table for array values.
/// Supports 1D and 2D arrays.
class TensorPanel {

	final Composite composite;
	private final Text equationText;
	private final TableViewer table;

	private Dimension rowDim;
	private Dimension colDim;
	private final List<String[]> data = new ArrayList<>();

	/// The original cell that was set via {@link #setInput(Cell)}.
	private Cell originalCell;

	TensorPanel(Composite parent, FormToolkit tk) {
		composite = UI.composite(parent, tk);
		UI.gridLayout(composite, 2);
		UI.gridData(composite, true, true);

		equationText = UI.labeledMultiText(composite, tk, "Equation", 80);
		equationText.setEditable(false);

		var tableLabel = UI.label(composite, tk, "Array values");
		var gd = UI.gridData(tableLabel, true, false);
		gd.horizontalSpan = 2;

		table = new TableViewer(composite,
			SWT.BORDER | SWT.FULL_SELECTION);
		var tableGd = UI.gridData(table.getControl(), true, true);
		tableGd.horizontalSpan = 2;
		tableGd.heightHint = 200;
		table.getTable().setHeaderVisible(true);
		table.getTable().setLinesVisible(true);
		table.setContentProvider(ArrayContentProvider.getInstance());
	}

	void setInput(Cell cell) {
		originalCell = cell;
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

	Cell getCell() {
		return originalCell != null ? originalCell : Cell.empty();
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

	/// Rebuilds table columns and data from current dimensions.
	private void rebuildTable() {
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

		// value columns (read-only, no editing support)
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
		}

		table.setInput(data);
	}
}
