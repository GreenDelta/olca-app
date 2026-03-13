package org.openlca.app.editors.sd.editor.graph.view;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.ToolbarLayout;
import org.openlca.sd.model.LookupFunc;
import org.openlca.sd.model.Tensor;
import org.openlca.sd.model.Var;
import org.openlca.sd.model.cells.BoolCell;
import org.openlca.sd.model.cells.Cell;
import org.openlca.sd.model.cells.EmptyCell;
import org.openlca.sd.model.cells.EqnCell;
import org.openlca.sd.model.cells.LookupCell;
import org.openlca.sd.model.cells.LookupEqnCell;
import org.openlca.sd.model.cells.NonNegativeCell;
import org.openlca.sd.model.cells.NumCell;
import org.openlca.sd.model.cells.TensorCell;
import org.openlca.sd.model.cells.TensorEqnCell;

class VarToolTip extends Figure {

	VarToolTip(Var v) {
		var layout = new ToolbarLayout();
		layout.setSpacing(2);
		layout.setMinorAlignment(ToolbarLayout.ALIGN_TOPLEFT);
		setLayoutManager(layout);
		setBorder(new MarginBorder(5));
		add(new Label(contentOf(v.def())));
	}

	private String contentOf(Cell cell) {
		return switch (cell) {
			case BoolCell(boolean bool) -> Boolean.toString(bool);
			case EmptyCell ignore -> "<empty>";
			case EqnCell(String eqn) -> eqn;
			case LookupCell ignore -> "x -> y";
			case LookupEqnCell(String eqn, LookupFunc ignore) -> "y := " + eqn;
			case NonNegativeCell(Cell inner) -> "|" + contentOf(inner) + "|";
			case NumCell(double num) -> Double.toString(num);
			case TensorCell(Tensor tensor) -> {
				if (tensor == null) yield "<empty>";
				var buf = new StringBuilder();
				for (int i : tensor.shape()) {
					if (!buf.isEmpty()) {
						buf.append(" * ");
					}
					buf.append(i);
				}
				yield "Array with " + buf + " elements";
			}
			case TensorEqnCell(Cell eqn, Tensor ignore) -> contentOf(eqn);
			case null -> "<empty>";
		};
	}

}
