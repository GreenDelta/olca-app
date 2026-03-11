package org.openlca.app.editors.sd.editor.graph.model;

import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.sd.model.Rect;
import org.openlca.sd.model.SdModel;
import org.openlca.sd.model.Var;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class VarNode implements NotifySupport {

	private final Var variable;
	private final SdModel model;
	private final Rectangle bounds = new Rectangle();

	private final List<VarLink> sourceLinks = new ArrayList<>();
	private final List<VarLink> targetLinks = new ArrayList<>();

	private final Notifier notifier = new Notifier();

	public VarNode(Var variable, SdModel model) {
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

	public List<VarLink> sourceLinks() {
		return sourceLinks;
	}

	public List<VarLink> targetLinks() {
		return targetLinks;
	}

	public void addSourceLink(VarLink link) {
		if (link == null) return;
		sourceLinks.add(link);
		notifier.fire();
	}

	public void addTargetLink(VarLink link) {
		if (link == null) return;
		targetLinks.add(link);
		notifier.fire();
	}

	public void removeSourceLink(VarLink link) {
		if (link == null) return;
		if (sourceLinks.remove(link)) {
			notifier.fire();
		}
	}

	public void removeTargetLink(VarLink link) {
		if (link == null) return;
		if (targetLinks.remove(link)) {
			notifier.fire();
		}
	}

	public void clearLinks() {
		sourceLinks.clear();
		targetLinks.clear();
		notifier.fire();;
	}

	public String name() {
		return variable.name() != null
			? variable.name().label()
			: "";
	}
}
