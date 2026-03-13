package org.openlca.app.editors.sd.editor.graph.model;

import org.eclipse.draw2d.geometry.Rectangle;

import java.util.ArrayList;
import java.util.List;

abstract sealed class SdNode implements NotifySupport permits VarNode {

	private final Notifier notifier = new Notifier();
	private final Rectangle bounds = new Rectangle();

	private final List<VarLink> sourceLinks = new ArrayList<>();
	private final List<VarLink> targetLinks = new ArrayList<>();

	@Override
	public final Notifier notifier() {
		return notifier;
	}

	public void moveTo(Rectangle rect) {
		if (rect == null)	return;
		bounds.setBounds(rect);
		notifier.fire();
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
		notifier.fire();
	}
}
