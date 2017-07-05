package org.openlca.app.editors.graphical.model;

import org.eclipse.draw2d.AbstractConnectionAnchor;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;

class LinkAnchor extends AbstractConnectionAnchor {

	private final boolean forInput;
	private final ProcessNode node;

	/**
	 * Creates a link anchor for an output. The exchange node can be null if the
	 * process is minimized.
	 */
	static LinkAnchor forOutput(ProcessNode p, ExchangeNode e) {
		if (p == null)
			return null;
		return new LinkAnchor(p, e, false);
	}

	/**
	 * Creates a link anchor for an output. The exchange node can be null if the
	 * process is minimized.
	 */
	static LinkAnchor forInput(ProcessNode p, ExchangeNode e) {
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
	public Point getLocation(Point reference) {
		int hTrans = 0;
		if (!node.isMinimized()) {
			hTrans = ProcessFigure.MARGIN_WIDTH + 1;
			if (forInput) {
				hTrans *= -1;
			}
		}
		Rectangle r = getOwner().getBounds().getCopy();
		r.translate(hTrans, 0);
		getOwner().translateToAbsolute(r);
		Point location = null;
		if (forInput) {
			location = r.getLeft();
		} else {
			location = r.getRight();
		}
		return location;
	}

}
