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
import org.openlca.sd.model.LookupFunc;
import org.openlca.sd.model.cells.Cell;
import org.openlca.sd.model.cells.LookupCell;
import org.openlca.sd.model.cells.LookupEqnCell;
import org.openlca.sd.model.cells.NonNegativeCell;

/// Panel for editing lookup function cells: LookupCell, LookupEqnCell.
/// Shows an equation text box and a table with x/y value pairs.
class LookupPanel {

	final Composite composite;
	private final Text equationText;
	private final TableViewer table;
	private final List<double[]> rows = new ArrayList<>();

	LookupPanel(Composite parent, FormToolkit tk) {
		composite = UI.composite(parent, tk);
		UI.gridLayout(composite, 2);
		UI.gridData(composite, true, true);

		equationText = UI.labeledMultiText(composite, tk, "Equation", 80);

		// lookup table label spans both columns
		var tableLabel = UI.label(composite, tk, "Lookup values");
		var gd = UI.gridData(tableLabel, true, false);
		gd.horizontalSpan = 2;

		table = createTable(composite);
		var tableGd = UI.gridData(table.getControl(), true, true);
		tableGd.horizontalSpan = 2;
		tableGd.heightHint = 200;

		createContextMenu();
	}

	private TableViewer createTable(Composite parent) {
		var viewer = new TableViewer(parent,
			SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		var t = viewer.getTable();
		t.setHeaderVisible(true);
		t.setLinesVisible(true);

		var xCol = new TableViewerColumn(viewer, SWT.NONE);
		xCol.getColumn().setText("x");
		xCol.getColumn().setWidth(150);
		xCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof double[] row) {
					return Double.toString(row[0]);
				}
				return "";
			}
		});
		xCol.setEditingSupport(new ValueEditingSupport(viewer, 0));

		var yCol = new TableViewerColumn(viewer, SWT.NONE);
		yCol.getColumn().setText("y");
		yCol.getColumn().setWidth(150);
		yCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof double[] row) {
					return Double.toString(row[1]);
				}
				return "";
			}
		});
		yCol.setEditingSupport(new ValueEditingSupport(viewer, 1));

		viewer.setContentProvider(ArrayContentProvider.getInstance());
		return viewer;
	}

	private void createContextMenu() {
		var menu = new Menu(table.getTable());
		var addItem = new MenuItem(menu, SWT.PUSH);
		addItem.setText("Add row");
		Controls.onSelect(addItem, e -> {
			rows.add(new double[]{0, 0});
			table.setInput(rows);
		});

		var removeItem = new MenuItem(menu, SWT.PUSH);
		removeItem.setText("Remove selected rows");
		Controls.onSelect(removeItem, e -> {
			var sel = table.getTable().getSelectionIndices();
			// remove from end to start to keep indices valid
			for (int i = sel.length - 1; i >= 0; i--) {
				if (sel[i] >= 0 && sel[i] < rows.size()) {
					rows.remove(sel[i]);
				}
			}
			table.setInput(rows);
		});

		table.getTable().setMenu(menu);
	}

	void setInput(Cell cell) {
		var unwrapped = cell instanceof NonNegativeCell(Cell inner)
			? inner
			: cell;

		rows.clear();
		switch (unwrapped) {
			case LookupEqnCell(String eqn, LookupFunc func) -> {
				equationText.setText(eqn != null ? eqn : "");
				fillRows(func);
			}
			case LookupCell(LookupFunc func) -> {
				equationText.setText("");
				fillRows(func);
			}
			default -> equationText.setText("");
		}
		table.setInput(rows);
	}

	private void fillRows(LookupFunc func) {
		if (func == null)
			return;
		var xs = func.xs();
		var ys = func.ys();
		var n = Math.min(xs.length, ys.length);
		for (int i = 0; i < n; i++) {
			rows.add(new double[]{xs[i], ys[i]});
		}
	}

	Cell getCell() {
		var func = buildFunc();
		var eqn = equationText.getText().trim();
		if (eqn.isEmpty()) {
			return new LookupCell(func);
		}
		return new LookupEqnCell(eqn, func);
	}

	private LookupFunc buildFunc() {
		var xs = new double[rows.size()];
		var ys = new double[rows.size()];
		for (int i = 0; i < rows.size(); i++) {
			xs[i] = rows.get(i)[0];
			ys[i] = rows.get(i)[1];
		}
		return new LookupFunc(LookupFunc.Type.CONTINUOUS, xs, ys);
	}

	Text equationText() {
		return equationText;
	}

	private class ValueEditingSupport extends EditingSupport {

		private final int colIndex;

		ValueEditingSupport(TableViewer viewer, int colIndex) {
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
			if (element instanceof double[] row) {
				return Double.toString(row[colIndex]);
			}
			return "0";
		}

		@Override
		protected void setValue(Object element, Object value) {
			if (element instanceof double[] row) {
				try {
					row[colIndex] = Double.parseDouble(value.toString());
				} catch (NumberFormatException e) {
					// ignore invalid input
				}
				table.refresh();
			}
		}
	}
}
