/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.productsystem.graphical.model;

import org.eclipse.draw2d.AbstractConnectionAnchor;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * 
 * Implementation of {@link AbstractConnectionAnchor} for a
 * {@link ConnectionLink} between two {@link ExchangeNode}s
 * 
 * @author Sebastian Greve
 * 
 */
public class ExchangeAnchor extends AbstractConnectionAnchor {

	/**
	 * Defines if the exchange anchor is on the left or on the right side of the
	 * {@link ExchangeFigure}
	 */
	private final boolean left;

	/**
	 * Defines if the process is minimized. If not, the anchors location has to
	 * be moved to fit to the {@link ProcessFigure} bounds
	 */
	private final boolean processMinimized;

	/**
	 * Constructor of a new ExchangeAnchor
	 * 
	 * @param figure
	 *            - owner figure of this anchor
	 * @param processMinimized
	 *            Defines if the process is minimized. If not, the anchors
	 *            location has to be moved to fit to the {@link ProcessFigure}
	 *            bounds
	 * @param left
	 *            Defines if the exchange anchor is on the left or on the right
	 *            side of the {@link ExchangeFigure}
	 */
	public ExchangeAnchor(final Figure figure, final boolean processMinimized,
			final boolean left) {
		super(figure);
		this.left = left;
		this.processMinimized = processMinimized;
	}

	@Override
	public Point getLocation(final Point reference) {
		int vTrans = 0;
		if (!processMinimized) {
			if (left) {
				vTrans += -6;
			} else {
				vTrans += 6;
			}
		}
		final Rectangle r = getOwner().getBounds().getCopy();
		r.translate(vTrans, 0);
		getOwner().translateToAbsolute(r);
		Point location = null;
		if (left) {
			location = r.getLeft();
		} else {
			location = r.getRight();
		}
		return location;
	}

}
