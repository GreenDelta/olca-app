package org.openlca.app.editors.graphical.model;

import java.util.Objects;

import org.eclipse.draw2d.AbstractConnectionAnchor;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;

class ConnectionLinkAnchor extends AbstractConnectionAnchor {

	public static final int SOURCE_ANCHOR = 1;
	public static final int TARGET_ANCHOR = 2;
	private int type;
	private ProcessNode node;
	private ConnectionLink link;

	ConnectionLinkAnchor(ProcessNode node, ConnectionLink link, int type) {
		super(node.isMinimized() ? node.getFigure()
				: (type == SOURCE_ANCHOR ? node.getOutputNode(
						link.getProcessLink().flowId).getFigure() : node
								.getInputNode(link.getProcessLink().flowId)
								.getFigure()));
		this.node = node;
		this.link = link;
		this.type = type;
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
		if (type == TARGET_ANCHOR)
			location = r.getLeft();
		else if (type == SOURCE_ANCHOR)
			location = r.getRight();
		return location;
	}

}
