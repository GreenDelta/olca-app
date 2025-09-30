package org.openlca.app.editors.sd.editor;

import org.openlca.sd.eqn.Var;
import org.openlca.sd.eqn.Var.Aux;
import org.openlca.sd.eqn.Var.Rate;
import org.openlca.sd.eqn.Var.Stock;

class Util {

	private Util() {
	}

	static String typeOf(Var var) {
		return switch (var) {
			case Stock ignored -> "Stock";
			case Aux ignored -> "Aux";
			case Rate ignored -> "Rate";
			case null -> "None";
		};
	}
}
