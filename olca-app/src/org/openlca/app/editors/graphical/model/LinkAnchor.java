package org.openlca.app.editors.graphical.model;

import org.eclipse.draw2d.AbstractConnectionAnchor;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;

class LinkAnchor extends AbstractConnectionAnchor {

	private static final int SOURCE_ANCHOR = 1;
	private static final int TARGET_ANCHOR = 2;
	private final int type;
	private final ProcessNode node;

	static LinkAnchor newSourceAnchor(Link link) {
		ProcessNode process = link.sourceNode;
		ExchangeNode eNode = process.getOutput(link.processLink);
		return newSourceAnchor(link.sourceNode, eNode);
	}

	static LinkAnchor newSourceAnchor(ProcessNode node, ExchangeNode eNode) {
		return new LinkAnchor(node, eNode, SOURCE_ANCHOR);
	}

	static LinkAnchor newTargetAnchor(Link link) {
		ExchangeNode eNode = link.targetNode.getInput(link.processLink);
		return newTargetAnchor(link.targetNode, eNode);
	}

	static LinkAnchor newTargetAnchor(ProcessNode node, ExchangeNode eNode) {
		return new LinkAnchor(node, eNode, TARGET_ANCHOR);
	}

	private LinkAnchor(ProcessNode pNode, ExchangeNode eNode, int type) {
		super(pNode.isMinimized() ? pNode.figure : eNode.figure);
		this.node = pNode;
		this.type = type;
	}

	@Override
	public Point getLocation(Point reference) {
		int hTrans = 0;
		if (!node.isMinimized()) {
			hTrans = ProcessFigure.MARGIN_WIDTH + 1;
			if (type == TARGET_ANCHOR) {
				hTrans *= -1;
			}
		}
		Rectangle r = getOwner().getBounds().getCopy();
		r.translate(hTrans, 0);
		getOwner().translateToAbsolute(r);
		Point location = null;
		if (type == TARGET_ANCHOR) {
			location = r.getLeft();
		} else if (type == SOURCE_ANCHOR) {
			location = r.getRight();
		}
		return location;
	}

}
