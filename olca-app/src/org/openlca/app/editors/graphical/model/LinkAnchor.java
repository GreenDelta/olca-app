package org.openlca.app.editors.graphical.model;

import java.util.Objects;

import org.eclipse.draw2d.AbstractConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.app.editors.graphical.GraphUtil;
import org.openlca.core.model.ProcessLink;

class LinkAnchor extends AbstractConnectionAnchor {

	private static final int SOURCE_ANCHOR = 1;
	private static final int TARGET_ANCHOR = 2;

	private int type;
	private ProcessNode node;
	private ConnectionLink link;

	private LinkAnchor(IFigure figure, ProcessNode node, ConnectionLink link) {
		super(figure);
		this.node = node;
		this.link = link;
	}

	static LinkAnchor createSourceAnchor(ProcessNode node, ConnectionLink link) {
		return createAnchor(node, link, SOURCE_ANCHOR);
	}

	static LinkAnchor createSourceAnchor(ExchangeNode node, ConnectionLink link) {
		ProcessNode p = GraphUtil.getProcessNode(node);
		return createAnchor(p, link, SOURCE_ANCHOR);
	}

	static LinkAnchor createTargetAnchor(ProcessNode node, ConnectionLink link) {
		return createAnchor(node, link, TARGET_ANCHOR);
	}

	static LinkAnchor createTargetAnchor(ExchangeNode node, ConnectionLink link) {
		ProcessNode p = GraphUtil.getProcessNode(node);
		return createAnchor(p, link, TARGET_ANCHOR);
	}

	private static LinkAnchor createAnchor(ProcessNode node,
			ConnectionLink link, int type) {
		if (node == null || link == null)
			return null;
		IFigure figure = null;
		ProcessLink pLink = link.processLink;
		if (node.isMinimized()) {
			figure = node.getFigure();
		} else if (type == SOURCE_ANCHOR) {
			ExchangeNode provider = node.getProviderNode(pLink.flowId);
			figure = provider != null ? provider.getFigure() : null;
		} else if (type == TARGET_ANCHOR) {
			ExchangeNode exchange = node.getExchangeNode(pLink.exchangeId);
			figure = exchange != null ? exchange.getFigure() : null;
		}
		if (figure == null)
			return null;
		LinkAnchor anchor = new LinkAnchor(figure, node, link);
		anchor.type = type;
		return anchor;
	}

	@Override
	public Point getLocation(Point reference) {
		int hTrans = 0;
		if (!node.isMinimized()) {
			if (Objects.equals(link.targetNode, node))
				hTrans -= ProcessFigure.MARGIN_WIDTH + 1;
			else if (Objects.equals(link.sourceNode, node))
				hTrans += ProcessFigure.MARGIN_WIDTH + 1;
		}
		Rectangle r = getOwner().getBounds().getCopy();
		r.translate(hTrans, 0);
		getOwner().translateToAbsolute(r);
		return type == TARGET_ANCHOR ? r.getLeft() : r.getRight();
	}

}
