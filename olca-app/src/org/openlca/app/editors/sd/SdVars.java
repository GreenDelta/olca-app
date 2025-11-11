package org.openlca.app.editors.sd;

import org.openlca.commons.Strings;
import org.openlca.sd.eqn.LookupFunc;
import org.openlca.sd.eqn.Tensor;
import org.openlca.sd.eqn.Var;
import org.openlca.sd.eqn.Var.Aux;
import org.openlca.sd.eqn.Var.Rate;
import org.openlca.sd.eqn.Var.Stock;
import org.openlca.sd.eqn.cells.BoolCell;
import org.openlca.sd.eqn.cells.Cell;
import org.openlca.sd.eqn.cells.EmptyCell;
import org.openlca.sd.eqn.cells.EqnCell;
import org.openlca.sd.eqn.cells.LookupCell;
import org.openlca.sd.eqn.cells.LookupEqnCell;
import org.openlca.sd.eqn.cells.NonNegativeCell;
import org.openlca.sd.eqn.cells.NumCell;
import org.openlca.sd.eqn.cells.TensorCell;
import org.openlca.sd.eqn.cells.TensorEqnCell;

public class SdVars {

	private SdVars() {
	}

	public static String typeOf(Var var) {
		return switch (var) {
			case Stock ignored -> "Stock";
			case Aux ignored -> "Aux";
			case Rate ignored -> "Rate";
			case null -> "None";
		};
	}

	public static String cellTypeOf(Var var) {
		return var != null
			? typeOf(var.def())
			: "None";
	}

	private static String typeOf(Cell cell) {
		return switch (cell) {
			case EmptyCell() -> "Empty";
			case NumCell(double num) -> "Number: " + num;
			case BoolCell(boolean b) -> "Boolean: " + b;
			case LookupCell(LookupFunc ignore) -> "Lookup function";
			case EqnCell(String eqn) -> "Equation: " + Strings.cutEnd(eqn, 50);
			case TensorCell(Tensor t) -> "Tensor: " + t.dimensions().size() + "d";
			case TensorEqnCell(Cell eqn, Tensor t) ->
				"Tensor equation: " + t.dimensions().size() + "d; " + typeOf(eqn);
			case LookupEqnCell(String eqn, LookupFunc ignore) ->
				"Lookup equation: " + Strings.cutEnd(eqn, 50);
			case NonNegativeCell(Cell inner) -> "Non-negative: " + typeOf(inner);
			case null -> "None";
		};
	}

}
