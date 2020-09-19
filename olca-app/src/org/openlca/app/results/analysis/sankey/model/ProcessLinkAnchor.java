package org.openlca.app.results.analysis.sankey.model;

import org.eclipse.draw2d.AbstractConnectionAnchor;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * The anchor of a process link to a process figure.
 */
public class ProcessLinkAnchor extends AbstractConnectionAnchor {

	private final boolean recipient;

	public ProcessLinkAnchor(Link link, boolean recipient) {
		super(recipient
				? link.targetNode.figure
				: link.sourceNode.figure);
		this.recipient = recipient;
	}

	@Override
	public Point getLocation(Point reference) {
		int horizontalTranslation = 0;
		Rectangle r = getOwner().getBounds().getCopy();
		r.translate(horizontalTranslation, recipient ? -1 : 0);
		getOwner().translateToAbsolute(r);
		return recipient
				? r.getBottom()
				: r.getTop();
	}

}
