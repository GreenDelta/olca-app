/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.analyze.sankey;

import java.util.List;

import org.eclipse.draw2d.AbstractConnectionAnchor;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.core.editors.productsystem.graphical.model.ExchangeNode;

/**
 * 
 * Implementation of {@link AbstractConnectionAnchor} for a
 * {@link ConnectionLink} between two {@link ExchangeNode}s
 * 
 * @author Sebastian Greve
 * 
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
	public Point getLocation(final Point reference) {
		final List<ConnectionLink> links = recipient ? processNode
				.getIncomingLinks() : processNode.getOutgoingLinks();
		int vTrans = 0;
		if (links.size() > 1) {
			int index = getPosition(links);
			int totalWidth = getTotalWidth(links);
			// calculate vertical translation
			vTrans = -(totalWidth - 1) / 2;
			for (int i = 0; i < index; i++) {
				double value = links.get(i).getRatio()
						* ConnectionLink.MAXIMIM_WIDTH;
				int width = (int) (value >= 0 ? Math.floor(Math.abs(value))
						: Math.ceil(Math.abs(value)));
				if (width <= 1) {
					width = 0;
				}
				vTrans += width;
			}
			double value = link.getRatio() * ConnectionLink.MAXIMIM_WIDTH;
			int width = (int) (value >= 0 ? Math.floor(Math.abs(value)) : Math
					.ceil(Math.abs(value)));
			vTrans += Math.floor(width / 2);
		}

		// translate
		Rectangle r = getOwner().getBounds().getCopy();
		r.translate(vTrans, recipient ? -1 : 0);
		getOwner().translateToAbsolute(r);

		Point result = null;
		if (recipient) {
			result = r.getBottom();
		} else {
			result = r.getTop();
		}
		return result;
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
			if (width < 1) {
				width = 1;
			}
			totalWidth += width;
		}
		return totalWidth;
	}
}
