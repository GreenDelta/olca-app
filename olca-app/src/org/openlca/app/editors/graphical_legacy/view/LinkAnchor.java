package org.openlca.app.editors.graphical_legacy.view;

import org.eclipse.draw2d.AbstractConnectionAnchor;
import org.eclipse.draw2d.geometry.Point;
import org.openlca.app.editors.graphical_legacy.model.ExchangeNode;
import org.openlca.app.editors.graphical_legacy.model.ProcessNode;

public class LinkAnchor extends AbstractConnectionAnchor {

	private final boolean forInput;
	private final ProcessNode node;

	/**
	 * Creates a link anchor for an output. The exchange node can be null if the
	 * process is minimized.
	 */
	public static LinkAnchor forOutput(ProcessNode p, ExchangeNode e) {
		if (p == null)
			return null;
		return new LinkAnchor(p, e, false);
	}

	/**
	 * Creates a link anchor for an output. The exchange node can be null if the
	 * process is minimized.
	 */
	public static LinkAnchor forInput(ProcessNode p, ExchangeNode e) {
		if (p == null)
			return null;
		return new LinkAnchor(p, e, true);
	}

	private LinkAnchor(ProcessNode pNode, ExchangeNode eNode, boolean forInput) {
		super(pNode.isMinimized() || eNode == null
				? pNode.figure
				: eNode.figure);
		this.node = pNode;
		this.forInput = forInput;
	}

	@Override
	public Point getLocation(Point ref) {
		var owner = getOwner();
		if (owner == null)
			return ref;

		// for processes it is just the middle
		// of the left or right side
		var bounds = owner.getBounds();
		var point = forInput
				? bounds.getLeft()
				: bounds.getRight();
		owner.translateToAbsolute(point);
		if ((owner instanceof ProcessFigure)
				|| node.isMinimized()
				|| !(owner instanceof ExchangeFigure))
			return point;

		// for exchanges we move the anchor to the
		// left or right side of the surrounding
		// process box

		// find the surrounding process figure
		var process = owner.getParent();
		int depth = 0; // just in case of cycles
		while (process != null && depth < 100) {
			if (process instanceof ProcessFigure)
				break;
			process = process.getParent();
			depth++;
		}
		if (process == null)
			return point;

		// set x to the right or left side of
		// the surrounding box
		var outerBounds = process.getBounds();
		var outer = forInput
			? outerBounds.getLeft()
			: outerBounds.getRight();
		process.translateToAbsolute(outer);
		point.x = outer.x;
		return point;
	}

}
