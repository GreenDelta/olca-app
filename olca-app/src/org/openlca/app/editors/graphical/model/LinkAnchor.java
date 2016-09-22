package org.openlca.app.editors.graphical.model;

import org.eclipse.draw2d.AbstractConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.app.editors.graphical.command.CreateLinkCommand;

class LinkAnchor extends AbstractConnectionAnchor {

	private static final int SOURCE_ANCHOR = 1;
	private static final int TARGET_ANCHOR = 2;
	private final int type;
	private final ProcessNode node;

	static LinkAnchor newSourceAnchor(Link link) {
		ExchangeNode eNode = link.sourceNode.getOutput(link.processLink.flowId);
		return newSourceAnchor(link.sourceNode, eNode);
	}

	static LinkAnchor newSourceAnchor(CreateLinkCommand cmd) {
		ExchangeNode eNode = cmd.sourceNode.getOutput(cmd.getLink().processLink.flowId);
		return newSourceAnchor(cmd.sourceNode, eNode);
	}

	static LinkAnchor newSourceAnchor(ProcessNode node, ExchangeNode eNode) {
		return newAnchor(node, eNode, SOURCE_ANCHOR);
	}

	static LinkAnchor newTargetAnchor(Link link) {
		ExchangeNode eNode = link.targetNode.getNode(link.processLink.exchangeId);
		return newTargetAnchor(link.targetNode, eNode);
	}

	static LinkAnchor newTargetAnchor(CreateLinkCommand cmd) {
		ProcessNode node = cmd.targetNode.parent();
		return newTargetAnchor(node, cmd.targetNode);
	}

	static LinkAnchor newTargetAnchor(ProcessNode node, ExchangeNode eNode) {
		return newAnchor(node, eNode, TARGET_ANCHOR);
	}

	private static LinkAnchor newAnchor(ProcessNode node, ExchangeNode eNode, int type) {
		IFigure figure = node.figure;
		if (!node.isMinimized())
			figure = eNode.figure;
		return new LinkAnchor(node, figure, type);
	}

	private LinkAnchor(ProcessNode node, IFigure figure, int type) {
		super(figure);
		this.node = node;
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
