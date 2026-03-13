package org.openlca.app.editors.sd.editor.graph.model;

import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.sd.model.Rect;
import org.openlca.sd.model.SdModel;
import org.openlca.sd.model.Var;

import java.util.Objects;

public final class VarNode extends SdNode {

	private final Var variable;
	private final SdModel model;

	public VarNode(Var variable, SdModel model) {
		this.variable = Objects.requireNonNull(variable);
		this.model = Objects.requireNonNull(model);
	}

	public Var variable() {
		return variable;
	}

	@Override
	public void moveTo(Rectangle rect) {
		if (rect == null) return;
		model.positions().put(
			variable.name(), new Rect(rect.x, rect.y, rect.width, rect.height));
		super.moveTo(rect);
	}

	public String name() {
		return variable.name() != null
			? variable.name().label()
			: "";
	}
}
