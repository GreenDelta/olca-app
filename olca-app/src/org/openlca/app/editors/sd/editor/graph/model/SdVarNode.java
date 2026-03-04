package org.openlca.app.editors.sd.editor.graph.model;

import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.sd.model.Rect;
import org.openlca.sd.model.SdModel;
import org.openlca.sd.model.Var;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SdVarNode implements NotifySupport {

	private final Var variable;
	private final SdModel model;
	private final Rectangle bounds = new Rectangle();

	private final List<SdVarLink> sourceLinks = new ArrayList<>();
	private final List<SdVarLink> targetLinks = new ArrayList<>();

	private final Notifier notifier = new Notifier();

	public SdVarNode(Var variable, SdModel model) {
		this.variable = Objects.requireNonNull(variable);
		this.model = Objects.requireNonNull(model);
	}

	@Override
	public Notifier notifier() {
		return notifier;
	}

	public void moveTo(Rectangle rect) {
		if (rect == null)
			return;
		bounds.setBounds(rect);
		model.positions().put(
			variable.name(), new Rect(rect.x, rect.y, rect.width, rect.height));
		notifier.fire();
	}

	public Var variable() {
		return variable;
	}

	public Rectangle bounds() {
		return bounds;
	}

	public List<SdVarLink> sourceLinks() {
		return sourceLinks;
	}

	public List<SdVarLink> targetLinks() {
		return targetLinks;
	}

	public String name() {
		return variable != null && variable.name() != null
			? variable.name().label()
			: "";
	}
}
