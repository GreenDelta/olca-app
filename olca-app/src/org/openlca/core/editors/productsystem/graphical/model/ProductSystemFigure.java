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

import org.eclipse.draw2d.Border;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.swt.graphics.Color;
import org.openlca.core.editors.productsystem.graphical.GraphLayoutManager;
import org.openlca.core.editors.productsystem.graphical.GraphLayoutType;

/**
 * Figure of a {@link ProductSystemNode}
 * 
 * @author Sebastian Greve
 * 
 */
public class ProductSystemFigure extends Figure {

	/**
	 * True at beginning, false after the figure is first time painted
	 */
	private boolean firstTime = true;

	/**
	 * Constructor of a new {@link ProductSystemFigure}. Sets the foreground
	 * {@link Color} and the {@link Border}
	 */
	public ProductSystemFigure(final ProductSystemNode node) {
		setForegroundColor(ColorConstants.black);
		setBorder(new LineBorder(1));
		node.setFigure(this);
	}

	/**
	 * Disposes the figure
	 */
	public void dispose() {
		for (final Object figure : getChildren()) {
			if (figure instanceof ProcessFigure) {
				((ProcessFigure) figure).dispose();
			}
		}
		getChildren().clear();
	}

	/**
	 * Paints this Figure and its children. Also fires 'firstTimeInitialized'
	 * PropertyChangeEvent
	 * 
	 * @param graphics
	 *            The Graphics object used for painting
	 * @see #paintFigure(Graphics)
	 * @see #paintClientArea(Graphics)
	 * @see #paintBorder(Graphics)
	 */
	@Override
	public void paint(final Graphics graphics) {
		if (firstTime) {
			firePropertyChange("firstTimeInitialized", "not null", null);
		}
		super.paint(graphics);
		if (firstTime) {
			((GraphLayoutManager) getLayoutManager()).layout(this,
					GraphLayoutType.TreeLayout);
			firstTime = false;
		}
	}

}
