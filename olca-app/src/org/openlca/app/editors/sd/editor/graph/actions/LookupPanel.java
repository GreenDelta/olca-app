package org.openlca.app.editors.sd.editor.graph.actions;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.viewers.tables.modify.DoubleCellModifier;
import org.openlca.app.viewers.tables.modify.ModifySupport;
import org.openlca.commons.Strings;
import org.openlca.sd.model.LookupFunc;
import org.openlca.sd.model.cells.Cell;
import org.openlca.sd.model.cells.LookupCell;
import org.openlca.sd.model.cells.LookupEqnCell;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class LookupPanel {

	private final Composite composite;
	private final StyledText text;
	private final TableViewer table;
	private final List<Row> rows = new ArrayList<>();
	private LookupFunc.Type type = LookupFunc.Type.CONTINUOUS;

	LookupPanel(Composite parent, FormToolkit tk) {
		composite = UI.composite(parent, tk);
		UI.gridLayout(composite, 1, 5, 0);
		UI.gridData(composite, true, true);

		UI.label(composite, tk, "Equation for x");
		text = new StyledText(composite, SWT.BORDER | SWT.MULTI);
		UI.gridData(text, true, false).heightHint = 80;

		var comp = UI.composite(composite, tk);
		UI.gridLayout(comp, 2, 10, 0);
		UI.gridData(comp, true, false);
		var combo = UI.labeledCombo(comp, tk, "Lookup values");


		table = createTable(composite);
		UI.gridData(table.getControl(), true, true).heightHint = 200;
	}

	public Composite composite() {
		return composite;
	}

	private TableViewer createTable(Composite parent) {
		var viewer = Tables.createViewer(parent, "x", "y");
		Tables.bindColumnWidths(viewer, 0.5, 0.5);
		viewer.setLabelProvider(new RowLabel());
		new ModifySupport<Row>(viewer)
			.bind("x", Row.modifierOfX())
			.bind("y", Row.modifierOfY());

		Actions.bind(viewer, Actions.create("Add row", Icon.ADD.descriptor(), () -> {
			rows.add(new Row(0, 0));
			viewer.setInput(rows);
		}));

		Actions.bind(viewer, Actions.create("Remove row(s)", Icon.DELETE.descriptor(), () -> {
			for (var o : Viewers.getAllSelected(viewer)) {
				if (o instanceof Row row) {
					rows.remove(row);
				}
				viewer.setInput(rows);
			}
		}));

		return viewer;
	}

	void setInput(Cell input) {
		text.setText(Panels.eqnOf(input));
		rows.clear();
		switch (input) {
			case LookupEqnCell(String ignore, LookupFunc func) -> Row.fill(func, rows);
			case LookupCell(LookupFunc func) -> Row.fill(func, rows);
			case null, default -> {
			}
		}
		table.setInput(rows);
	}

	Cell getCell() {
		rows.sort(Comparator.comparingDouble(ri -> ri.x));
		var xs = new double[rows.size()];
		var ys = new double[rows.size()];
		for (int i = 0; i < rows.size(); i++) {
			var row = rows.get(i);
			xs[i] = row.x;
			ys[i] = row.y;
		}
		var func = new LookupFunc(type, xs, ys);

		var eqn = text.getText().trim();
		return Strings.isBlank(eqn)
			? new LookupCell(func) // TODO: this is not valid
			: new LookupEqnCell(eqn, func);
	}

	private static class RowLabel extends LabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object o, int col) {
			return null;
		}

		@Override
		public String getColumnText(Object o, int col) {
			if (!(o instanceof Row row)) {
				return null;
			}
			var v = col == 0 ? row.x : row.y;
			return Double.toString(v);
		}
	}

	private static class Row {
		double x;
		double y;

		Row(double x, double y) {
			this.x = x;
			this.y = y;
		}

		static void fill(LookupFunc func, List<Row> rows) {
			if (func == null) return;
			var xs = func.xs();
			var ys = func.ys();
			if (xs == null || ys == null) return;
			int n = Math.min(xs.length, ys.length);
			for (int i = 0; i < n; i++) {
				rows.add(new Row(xs[i], ys[i]));
			}
		}

		static DoubleCellModifier<Row> modifierOfX() {
			return new DoubleCellModifier<>() {
				@Override
				public Double getDouble(Row row) {
					return row != null ? row.x : 0;
				}

				@Override
				public void setDouble(Row row, Double x) {
					if (row == null) return;
					row.x = x != null ? x : 0;
				}
			};
		}

		static DoubleCellModifier<Row> modifierOfY() {
			return new DoubleCellModifier<>() {
				@Override
				public Double getDouble(Row row) {
					return row != null ? row.y : 0;
				}

				@Override
				public void setDouble(Row row, Double y) {
					if (row == null) return;
					row.y = y != null ? y : 0;
				}
			};
		}
	}
}
