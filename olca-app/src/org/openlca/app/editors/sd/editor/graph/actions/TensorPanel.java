package org.openlca.app.editors.sd.editor.graph.actions;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.UI;
import org.openlca.sd.model.Dimension;
import org.openlca.sd.model.Id;
import org.openlca.sd.model.Subscript;
import org.openlca.sd.model.Tensor;
import org.openlca.sd.model.cells.Cell;
import org.openlca.sd.model.cells.TensorCell;
import org.openlca.sd.model.cells.TensorEqnCell;

import java.util.ArrayList;
import java.util.List;

final class TensorPanel extends Panel {

	private final StyledText text;
	private final TableViewer table;

	private Dimension rowDim;
	private Dimension colDim;
	private final List<Row> data = new ArrayList<>();
	private Cell input;

	TensorPanel(Composite parent, FormToolkit tk) {
		super(UI.composite(parent, tk));
		var comp = composite();
		UI.gridLayout(comp, 1, 5, 0);
		UI.gridData(comp, true, true);

		UI.label(comp, tk, "Equation for updating the array values");
		text = new StyledText(comp, SWT.BORDER | SWT.MULTI | SWT.WRAP);
		text.setEditable(false);
		text.setEnabled(false);
		var gd = UI.gridData(text, true, false);
		gd.heightHint = 80;
		// avoid horizontal growing
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=215997
		gd.widthHint = 1;

		UI.label(comp, tk, "Array values");
		table = new TableViewer(comp, SWT.BORDER | SWT.FULL_SELECTION);
		UI.gridData(table.getControl(), true, true).heightHint = 200;
		table.getTable().setHeaderVisible(true);
		table.getTable().setLinesVisible(true);
		table.setContentProvider(ArrayContentProvider.getInstance());
	}

	@Override
	public void setInput(Cell cell) {
		input = cell;
		text.setText(eqnOf(cell));
		var tensor = switch (cell) {
			case TensorEqnCell(Cell ignore, Tensor t) -> t;
			case TensorCell(Tensor t) -> t;
			case null, default -> null;
		};
		fillDataOf(tensor);
		rebuildTable();
	}

	@Override
	public Cell getCell() {
		return input != null ? input : Cell.empty();
	}

	private void fillDataOf(Tensor tensor) {
		data.clear();
		rowDim = null;
		colDim = null;
		if (tensor == null) return;
		var dims = tensor.dimensions();
		if (dims.isEmpty()) return;

		rowDim = dims.getFirst();
		colDim = dims.size() > 1 ? dims.get(1) : null;
		int cols = colDim != null ? colDim.size() : 1;

		for (var rowIdx : rowDim.elements()) {
			var eqns = new ArrayList<String>(cols);
			var row = new Row(rowIdx, eqns);
			data.add(row);
			var cell = tensor.get(Subscript.of(rowIdx));

			if (cols > 1 && cell instanceof TensorCell(Tensor sub)) {
				for (int c = 0; c < cols && c < sub.size(); c++) {
					eqns.add(eqnOf(sub.get(c)));
				}
			} else {
				eqns.add(eqnOf(cell));
			}
		}
	}


	private void rebuildTable() {
		for (var col : table.getTable().getColumns()) {
			col.dispose();
		}

		var idxLabel = rowDim != null && rowDim.name() != null
			? rowDim.name().label()
			: "Index";
		var idxCol = new TableViewerColumn(table, SWT.NONE);
		idxCol.getColumn().setText(idxLabel);
		idxCol.getColumn().setWidth(100);
		idxCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object o) {
				return o instanceof Row row
					? row.subscript().label()
					: null;
			}
		});

		if (colDim == null) {
			var valCol = new TableViewerColumn(table, SWT.NONE);
			valCol.getColumn().setText("Value");
			valCol.getColumn().setWidth(120);
			valCol.setLabelProvider(new CellLabel(0));
		} else {
			int idx = 0;
			for (var colIdx : colDim.elements()) {
				var col = new TableViewerColumn(table, SWT.NONE);
				col.getColumn().setText(colIdx.label());
				col.getColumn().setWidth(120);
				col.setLabelProvider(new CellLabel(idx));
				idx++;
			}
		}

		table.setInput(data);
	}

	private record Row(Id subscript, List<String> cells) {
	}

	private static class CellLabel extends ColumnLabelProvider {

		private final int idx;

		CellLabel(int idx) {
			this.idx = idx;
		}

		@Override
		public String getText(Object o) {
			if (!(o instanceof Row row)) return null;
			var cells = row.cells;
			return idx < cells.size()
				? cells.get(idx)
				: null;
		}
	}
}
