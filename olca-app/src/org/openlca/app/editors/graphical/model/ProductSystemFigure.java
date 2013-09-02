/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.editors.graphical.model;

import java.util.List;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.LineBorder;
import org.openlca.app.editors.graphical.layout.GraphLayoutManager;
import org.openlca.app.editors.graphical.layout.GraphLayoutType;

class ProductSystemFigure extends Figure {

	private boolean firstTime = true;

	ProductSystemFigure(ProductSystemNode node) {
		setForegroundColor(ColorConstants.black);
		setBorder(new LineBorder(1));
		node.setFigure(this);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<ProcessFigure> getChildren() {
		return super.getChildren();
	}

	@Override
	public GraphLayoutManager getLayoutManager() {
		return (GraphLayoutManager) super.getLayoutManager();
	}

	@Override
	public void paint(Graphics graphics) {
		if (firstTime)
			firePropertyChange("firstTimeInitialized", "not null", null);
		super.paint(graphics);
		if (firstTime) {
			getLayoutManager().layout(this, GraphLayoutType.TREE_LAYOUT);
			firstTime = false;
		}
	}

}
