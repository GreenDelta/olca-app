package org.openlca.app.editors.sd.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.sd.eqn.Cell;
import org.openlca.sd.eqn.Cell.BoolCell;
import org.openlca.sd.eqn.Cell.EmptyCell;
import org.openlca.sd.eqn.Cell.EqnCell;
import org.openlca.sd.eqn.Cell.LookupCell;
import org.openlca.sd.eqn.Cell.NonNegativeCell;
import org.openlca.sd.eqn.Cell.NumCell;
import org.openlca.sd.eqn.Cell.TensorCell;
import org.openlca.sd.eqn.LookupFunc;
import org.openlca.sd.eqn.Subscript;
import org.openlca.sd.eqn.Tensor;
import org.openlca.sd.eqn.Var;
import org.openlca.sd.eqn.Var.Aux;
import org.openlca.sd.eqn.Var.Rate;
import org.openlca.sd.eqn.Var.Stock;
import org.openlca.sd.eqn.Vars;
import org.openlca.util.Strings;

public class VarsPage extends FormPage {

	private final SdModelEditor editor;

	public VarsPage(SdModelEditor editor) {
		super(editor, "SdModelParametersPage", "Variables");
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.header(mForm, "System dynamics model: " + editor.modelName());
		var tk = mForm.getToolkit();
		var body = UI.body(form, tk);

		var section = UI.section(body, tk, "Variables");
		UI.gridData(section, true, true);
		var comp = UI.sectionClient(section, tk);
		UI.gridLayout(comp, 1);

		var table = Tables.createViewer(comp,
				"Type", "Name", "Definition", "Unit");
		Tables.bindColumnWidths(table, 0.10, 0.40, 0.40, 0.10);
		UI.gridData(table.getControl(), true, true);
		table.setLabelProvider(new Label());

		var vars = Vars.readFrom(editor.xmile()).orElse(ArrayList::new);
		vars.sort((vi, vj) -> {
			var ti = typeOf(vi);
			var tj = typeOf(vj);
			return ti.equals(tj)
					? Strings.compare(vi.name().label(), vj.name().label())
					: Strings.compare(tj, ti);
		});
		table.setInput(vars);
	}

	private static String typeOf(Var var) {
		return switch (var) {
			case Stock ignored -> "Stock";
			case Aux ignored -> "Aux";
			case Rate ignored -> "Rate";
			case null -> "None";
		};
	}

	private static class Label
			extends LabelProvider implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			return col == 0 ? Icon.FORMULA.get() : null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Var v))
				return null;
			return switch (col) {
				case 0 -> typeOf(v);
				case 1 -> v.name().label();
				case 2 -> value(v.def());
				default -> null;
			};
		}

		private String value(Cell cell) {
			return switch (cell) {
				case NumCell(double value) -> Double.toString(value);
				case TensorCell(Tensor t) -> toString(t);
				case LookupCell(
						String eqn, LookupFunc ignored, List<Subscript> ignore
				) -> "Lookup(" + Strings.cut(eqn, 75) + ")";
				case EqnCell(String eqn) -> Strings.cut(eqn, 80);
				case BoolCell(boolean b) -> Boolean.toString(b);
				case EmptyCell() -> " - ";
				case NonNegativeCell(Cell v) -> "NonNegative(" + value(v) + ")";
				case null -> "nil";
			};
		}

		private String toString(Tensor t) {
			var dims = t.dimensions()
					.stream()
					.map(dim -> dim.name().label())
					.toList();
			return "Array: " + String.join(" × ", dims);
		}

	}
}
