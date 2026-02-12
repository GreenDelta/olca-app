package org.openlca.app.editors.sd.editor.graph;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.sd.eqn.Var;

class StockModel {

	final Var.Stock stock;
	final Rectangle bounds = new Rectangle();

	private final List<Runnable> listeners = new ArrayList<>();

	StockModel(Var.Stock variable) {
		this.stock = variable;
	}

	void addListener(Runnable listener) {
		listeners.add(listener);
	}

	void removeListener(Runnable listener) {
		listeners.remove(listener);
	}

	void moveTo(Rectangle rect) {
		if (rect == null)
			return;
		bounds.setBounds(rect);
		for (var listener : listeners) {
			listener.run();
		}
	}

	String name() {
		return stock != null && stock.name() != null
			? stock.name().label()
			: "";
	}

}
