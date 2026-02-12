package org.openlca.app.editors.sd.editor.graph;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.sd.eqn.Var;

class StockModel {

	final Var.Stock stock;

	int x;
	int y;
	int width;
	int height;

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
		if (rect == null) return;
		this.x = rect.x;
		this.y = rect.y;
		this.width = rect.width;
		this.height = rect.height;
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
