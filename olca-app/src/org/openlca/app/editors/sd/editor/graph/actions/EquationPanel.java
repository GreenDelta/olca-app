package org.openlca.app.editors.sd.editor.graph.actions;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.UI;
import org.openlca.sd.model.cells.BoolCell;
import org.openlca.sd.model.cells.Cell;
import org.openlca.sd.model.cells.EqnCell;
import org.openlca.sd.model.cells.NonNegativeCell;
import org.openlca.sd.model.cells.NumCell;

/// Panel for editing simple equation cells: EqnCell, NumCell, BoolCell.
class EquationPanel {

	final Composite composite;
	private final Text equationText;

	EquationPanel(Composite parent, FormToolkit tk) {
		composite = UI.composite(parent, tk);
		UI.gridLayout(composite, 2);
		UI.gridData(composite, true, true);

		equationText = UI.labeledMultiText(composite, tk, "Equation", 200);
	}

	void setInput(Cell cell) {
		equationText.setText(cellToText(cell));
	}

	Cell getCell() {
		return Cell.of(equationText.getText());
	}

	Text equationText() {
		return equationText;
	}

	private String cellToText(Cell def) {
		return switch (def) {
			case BoolCell(boolean b) -> Boolean.toString(b);
			case NumCell(double num) -> Double.toString(num);
			case EqnCell(String eqn) -> eqn != null ? eqn : "";
			case NonNegativeCell(Cell value) -> cellToText(value);
			case null, default -> "";
		};
	}
}
