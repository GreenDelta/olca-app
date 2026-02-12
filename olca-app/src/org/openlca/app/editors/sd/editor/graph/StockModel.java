package org.openlca.app.editors.sd.editor.graph;

import org.openlca.sd.eqn.Var;

class StockModel {

	final Var.Stock stock;

	StockModel(Var.Stock variable) {
		this.stock = variable;
	}

	String name() {
		return stock != null && stock.name() != null
			? stock.name().label()
			: "";
	}

}
