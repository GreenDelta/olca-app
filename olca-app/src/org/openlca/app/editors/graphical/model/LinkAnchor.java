package org.openlca.app.editors.graphical.model;

import java.util.Objects;

import org.eclipse.draw2d.AbstractConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.app.editors.graphical.GraphUtil;
import org.openlca.core.model.ProcessLink;

class LinkAnchor extends AbstractConnectionAnchor {

	private static final int OUTPUT = 1;
	private static final int INPUT = 2;

	private int type;
	private ProcessNode node;
	private ConnectionLink link;

	private LinkAnchor(IFigure figure, ProcessNode node, ConnectionLink link) {
		super(figure);
		this.node = node;
		this.link = link;
	}

	static LinkAnchor createOutputAnchor(ProcessNode node, ConnectionLink link) {
		return createAnchor(node, link, OUTPUT);
	}

	static LinkAnchor createOutputAnchor(ExchangeNode node, ConnectionLink link) {
		ProcessNode p = GraphUtil.getProcessNode(node);
		return createAnchor(p, link, OUTPUT);
	}

	static LinkAnchor createInputAnchor(ProcessNode node, ConnectionLink link) {
		return createAnchor(node, link, INPUT);
	}

	static LinkAnchor createInputAnchor(ExchangeNode node, ConnectionLink link) {
		ProcessNode p = GraphUtil.getProcessNode(node);
		return createAnchor(p, link, INPUT);
	}

	private static LinkAnchor createAnchor(ProcessNode node,
			ConnectionLink link, int type) {
		if (node == null || link == null)
			return null;
		IFigure figure = null;
		ProcessLink pLink = link.processLink;
		if (node.isMinimized()) {
			figure = node.getFigure();
		} else if (type == OUTPUT) {
			ExchangeNode provider = node.getProviderNode(pLink.flowId);
			figure = provider != null ? provider.getFigure() : null;
		} else if (type == INPUT) {
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
			if (Objects.equals(link.exchange, node))
				hTrans -= ProcessFigure.MARGIN_WIDTH + 1;
			else if (Objects.equals(link.provider, node))
				hTrans += ProcessFigure.MARGIN_WIDTH + 1;
		}
		Rectangle r = getOwner().getBounds().getCopy();
		r.translate(hTrans, 0);
		getOwner().translateToAbsolute(r);
		return type == INPUT ? r.getLeft() : r.getRight();
	}

}
