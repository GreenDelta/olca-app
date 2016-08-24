package org.openlca.app.editors.graphical.model;

import org.eclipse.draw2d.AbstractConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.app.editors.graphical.command.CreateLinkCommand;
import org.openlca.core.model.ProcessLink;

class LinkAnchor extends AbstractConnectionAnchor {

	private static final int SOURCE_ANCHOR = 1;
	private static final int TARGET_ANCHOR = 2;
	private final int type;
	private final ProcessNode node;

	static LinkAnchor newSourceAnchor(Link link) {
		return newSourceAnchor(link.sourceNode, link.processLink);
	}

	static LinkAnchor newSourceAnchor(CreateLinkCommand cmd) {
		return newSourceAnchor(cmd.sourceNode, cmd.getLink().processLink);
	}

	static LinkAnchor newSourceAnchor(ProcessNode node, ProcessLink link) {
		return newAnchor(node, link, SOURCE_ANCHOR);
	}

	static LinkAnchor newTargetAnchor(Link link) {
		return newTargetAnchor(link.targetNode, link.processLink);
	}

	static LinkAnchor newTargetAnchor(CreateLinkCommand cmd) {
		return newTargetAnchor(cmd.targetNode, cmd.getLink().processLink);
	}

	static LinkAnchor newTargetAnchor(ProcessNode node, ProcessLink link) {
		return newAnchor(node, link, TARGET_ANCHOR);
	}

	private static LinkAnchor newAnchor(ProcessNode node, ProcessLink link, int type) {
		IFigure figure = node.figure;
		if (!node.isMinimized()) {
			ExchangeNode eNode = null;
			if (type == SOURCE_ANCHOR) {
				eNode = node.getOutputNode(link.flowId);
			} else {
				eNode = node.getExchangeNode(link.exchangeId);
			}
			figure = eNode.figure;
		}
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
