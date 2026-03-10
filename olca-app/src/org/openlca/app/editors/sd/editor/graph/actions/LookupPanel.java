package org.openlca.app.editors.sd.editor.graph.actions;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
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

final class LookupPanel extends Panel {

	private final StyledText text;
	private final TableViewer table;
	private final TypeCombo typeCombo;
	private final List<Row> rows = new ArrayList<>();

	LookupPanel(Composite parent, FormToolkit tk) {
		super(UI.composite(parent, tk));
		var comp = composite();
		UI.gridLayout(comp, 1, 5, 0);
		UI.gridData(comp, true, true);
		UI.label(comp, tk, "Equation for x");
		text = new StyledText(comp, SWT.BORDER | SWT.MULTI);
		var gd = UI.gridData(text, true, false);
		gd.heightHint = 50;
		// avoid horizontal growing
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=215997
		gd.widthHint = 1;

		text.addModifyListener($ -> checkValid());

		typeCombo = new TypeCombo(comp, this::checkValid);
		table = createTable(comp);
		UI.gridData(table.getControl(), true, true).heightHint = 150;
	}

	private TableViewer createTable(Composite parent) {
		var table = Tables.createViewer(parent, "x", "y");
		table.getTable().getColumn(0).setWidth(150);
		table.getTable().getColumn(1).setWidth(150);
		table.setLabelProvider(new RowLabel());
		new ModifySupport<Row>(table)
			.bind("x", Row.modifierOfX())
			.bind("y", Row.modifierOfY());

		var onAdd = Actions.create(
			"Add row", Icon.ADD.descriptor(), () -> {
				rows.add(new Row(0, 0));
				table.setInput(rows);
				checkValid();
			});
		var onDelete = Actions.create(
			"Remove row(s)", Icon.DELETE.descriptor(), () -> {
				for (var o : Viewers.getAllSelected(table)) {
					if (o instanceof Row row) {
						rows.remove(row);
					}
				}
				table.setInput(rows);
				checkValid();
			});
		Actions.bind(table, onAdd, onDelete);

		return table;
	}

	private void checkValid() {
		if (text == null) return;
		fireValid(!rows.isEmpty() && Strings.isNotBlank(text.getText()));
	}

	@Override
	public void setInput(Cell input) {
		text.setText(eqnOf(input));
		rows.clear();
		var func = switch (input) {
			case LookupEqnCell(String ignore, LookupFunc fn) -> fn;
			case LookupCell(LookupFunc fn) -> fn;
			case null, default -> null;
		};
		if (func != null) {
			Row.fill(func, rows);
			typeCombo.select(func.type());
		}
		table.setInput(rows);
	}

	@Override
	public Cell getCell() {
		rows.sort(Comparator.comparingDouble(ri -> ri.x));
		var xs = new double[rows.size()];
		var ys = new double[rows.size()];
		for (int i = 0; i < rows.size(); i++) {
			var row = rows.get(i);
			xs[i] = row.x;
			ys[i] = row.y;
		}
		var func = new LookupFunc(typeCombo.selected(), xs, ys);
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

	private static class TypeCombo {

		private final Combo combo;
		private LookupFunc.Type selected = LookupFunc.Type.CONTINUOUS;

		private final LookupFunc.Type[] types = {
			LookupFunc.Type.CONTINUOUS,
			LookupFunc.Type.EXTRAPOLATE,
			LookupFunc.Type.DISCRETE,
		};

		TypeCombo(Composite parent, Runnable onChange) {
			combo = new Combo(parent, SWT.READ_ONLY);
			UI.gridData(combo, true, false);
			var items = new String[]{
				"Lookup values - linear interpolation",
				"Lookup values - linear extrapolation",
				"Lookup values - discrete (next lower)"
			};
			combo.setItems(items);
			combo.select(0);
			select(LookupFunc.Type.CONTINUOUS);
			Controls.onSelect(combo, $ -> onChange.run());
		}

		void select(LookupFunc.Type type) {
			if (type == null) return;
			selected = type;
			int idx = 0;
			for (int i = 0; i < types.length; i++) {
				if (types[i] == type) {
					idx = i;
					break;
				}
			}
			combo.select(idx);
		}

		public LookupFunc.Type selected() {
			return selected;
		}
	}

}
