package org.openlca.app.editors.sd.editor.graph;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.sd.eqn.Var;

class VarModel {

	final Var variable;
	final Rectangle bounds = new Rectangle();

	final List<LinkModel> sourceLinks = new ArrayList<>();
	final List<LinkModel> targetLinks = new ArrayList<>();

	private final List<Runnable> listeners = new ArrayList<>();

	VarModel(Var variable) {
		this.variable = variable;
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
		return variable != null && variable.name() != null
			? variable.name().label()
			: "";
	}
}
