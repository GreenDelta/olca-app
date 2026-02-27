package org.openlca.app.editors.sd;

import org.openlca.commons.Strings;
import org.openlca.sd.model.Auxil;
import org.openlca.sd.model.LookupFunc;
import org.openlca.sd.model.Rate;
import org.openlca.sd.model.Stock;
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

public class SdVars {

	private SdVars() {
	}

	public static String typeOf(Var var) {
		return switch (var) {
			case Stock ignored -> "Stock";
			case Auxil ignored -> "Aux";
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
