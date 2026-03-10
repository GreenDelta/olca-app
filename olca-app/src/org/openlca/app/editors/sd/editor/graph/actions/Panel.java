package org.openlca.app.editors.sd.editor.graph.actions;

import org.eclipse.swt.widgets.Composite;
import org.openlca.sd.model.LookupFunc;
import org.openlca.sd.model.Tensor;
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


sealed abstract class Panel permits EquationPanel, LookupPanel, TensorPanel {

	private final Composite composite;
	private ChangeObserver onChange;

	Panel(Composite composite) {
		this.composite = composite;
	}

	final Composite composite() {
		return composite;
	}

	void onChange(ChangeObserver onChange) {
		this.onChange = onChange;
	}

	void fireValid(boolean b) {
		if (onChange != null) {
			onChange.reactOn(b);
		}
	}

	abstract void setInput(Cell cell);

	abstract Cell getCell();

	String eqnOf(Cell cell) {
		return switch (cell) {
			case BoolCell(boolean b) -> Boolean.toString(b);
			case EmptyCell ignore -> "";
			case EqnCell(String eqn) -> eqn != null ? eqn : "";
			case LookupCell ignore -> "";
			case LookupEqnCell(String eqn, LookupFunc ignore) -> eqn != null ? eqn : "";
			case NonNegativeCell(Cell value) -> eqnOf(value);
			case NumCell(double num) -> Double.toString(num);
			case TensorCell ignore -> "";
			case TensorEqnCell(Cell eqn, Tensor ignore) -> eqnOf(eqn);
			case null -> "";
		};
	}
}
