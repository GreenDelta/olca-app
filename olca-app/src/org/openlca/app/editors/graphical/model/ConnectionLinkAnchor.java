package org.openlca.app.editors.graphical.model;

import java.util.Objects;

import org.eclipse.draw2d.AbstractConnectionAnchor;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;

class ConnectionLinkAnchor extends AbstractConnectionAnchor {

	private ProcessNode node;
	private ConnectionLink link;

	ConnectionLinkAnchor(ProcessNode node, ConnectionLink link) {
		super(node.isMinimized() ? node.getFigure() : node.getExchangeNode(
				link.getProcessLink().getFlowId()).getFigure());
		this.node = node;
		this.link = link;
	}

	@Override
	public Point getLocation(Point reference) {
		int hTrans = 0;
		if (!node.isMinimized())
			if (Objects.equals(link.getTargetNode(), node))
				hTrans -= ProcessFigure.MARGIN_WIDTH + 1;
			else if (Objects.equals(link.getSourceNode(), node))
				hTrans += ProcessFigure.MARGIN_WIDTH + 1;

		Rectangle r = getOwner().getBounds().getCopy();
		r.translate(hTrans, 0);
		getOwner().translateToAbsolute(r);

		Point location = null;
		if (Objects.equals(link.getTargetNode(), node))
			location = r.getLeft();
		else if (Objects.equals(link.getSourceNode(), node))
			location = r.getRight();
		return location;
	}

}
