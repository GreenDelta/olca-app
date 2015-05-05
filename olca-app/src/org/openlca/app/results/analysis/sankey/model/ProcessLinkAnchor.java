package org.openlca.app.results.analysis.sankey.model;

import java.util.List;

import org.eclipse.draw2d.AbstractConnectionAnchor;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * The anchor of a process link to a process figure.
 */
public class ProcessLinkAnchor extends AbstractConnectionAnchor {

	private ConnectionLink link;
	private ProcessNode processNode;
	private boolean recipient;

	public ProcessLinkAnchor(ConnectionLink link, boolean recipient) {
		super(recipient ? link.getTargetNode().getFigure() : link
				.getSourceNode().getFigure());
		this.link = link;
		this.recipient = recipient;
		this.processNode = recipient ? link.getTargetNode() : link
				.getSourceNode();
	}

	@Override
	public Point getLocation(Point reference) {
		// TODO: currently there are problems with the reference process
		// links when using this translation function.
		// List<ConnectionLink> links = recipient ?
		// processNode.getIncomingLinks()
		// : processNode.getOutgoingLinks();
		// int horizontalTranslation = calculateTranslation(links);
		int horizontalTranslation = 0;
		Rectangle r = getOwner().getBounds().getCopy();
		r.translate(horizontalTranslation, recipient ? -1 : 0);
		getOwner().translateToAbsolute(r);
		Point result = null;
		if (recipient)
			result = r.getBottom();
		else
			result = r.getTop();
		return result;
	}

	private int calculateTranslation(List<ConnectionLink> links) {
		if (links.size() <= 1)
			return 0;
		int index = getPosition(links);
		int totalWidth = getTotalWidth(links);
		int translation = -(totalWidth - 1) / 2;
		for (int i = 0; i < index; i++) {
			double value = links.get(i).getRatio()
					* ConnectionLink.MAXIMIM_WIDTH;
			int width = (int) (value >= 0 ? Math.floor(Math.abs(value)) : Math
					.ceil(Math.abs(value)));
			if (width <= 1)
				width = 0;
			translation += width;
		}
		double value = link.getRatio() * ConnectionLink.MAXIMIM_WIDTH;
		int width = (int) (value >= 0 ? Math.floor(Math.abs(value)) : Math
				.ceil(Math.abs(value)));
		translation += Math.floor(width / 2);
		return translation;
	}

	private int getPosition(List<ConnectionLink> links) {
		for (int i = 0; i < links.size(); i++) {
			if (links.get(i) == link)
				return i;
		}
		return 0;
	}

	private int getTotalWidth(List<ConnectionLink> links) {
		int totalWidth = 0;
		for (int i = 0; i < links.size(); i++) {
			double value = links.get(i).getRatio()
					* ConnectionLink.MAXIMIM_WIDTH;
			int width = (int) (value >= 0 ? Math.floor(Math.abs(value)) : Math
					.ceil(Math.abs(value)));
			if (width < 1)
				width = 1;
			totalWidth += width;
		}
		return totalWidth;
	}
}
