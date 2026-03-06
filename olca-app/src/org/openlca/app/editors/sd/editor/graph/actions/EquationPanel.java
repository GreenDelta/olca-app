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

	private final Composite composite;
	private final Text text;

	EquationPanel(Composite parent, FormToolkit tk) {
		composite = UI.composite(parent, tk);
		UI.gridLayout(composite, 1, 5, 0);
		UI.gridData(composite, true, true);
		text = UI.multiText(composite, tk, 150);
		UI.gridData(text, true, true);
	}

	public Composite composite() {
		return composite;
	}

	void setInput(Cell cell) {
		text.setText(eqnOf(cell));
	}

	Cell getCell() {
		return Cell.of(text.getText());
	}

	Text equationText() {
		return text;
	}

	private String eqnOf(Cell def) {
		return switch (def) {
			case BoolCell(boolean b) -> Boolean.toString(b);
			case NumCell(double num) -> Double.toString(num);
			case EqnCell(String eqn) -> eqn != null ? eqn : "";
			case NonNegativeCell(Cell value) -> eqnOf(value);
			case null, default -> "";
		};
	}
}
