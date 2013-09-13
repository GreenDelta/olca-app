/*******************************************************************************
 * Copyright (c) 2007 - 2012 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.analysis.sankey;

import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.swt.graphics.Font;

public class ProductSystemEditPart extends AbstractGraphicalEditPart {

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.LAYOUT_ROLE, new LayoutPolicy());
	}

	@Override
	protected IFigure createFigure() {
		final ProductSystemFigure figure = new ProductSystemFigure(
				(ProductSystemNode) getModel());
		figure.setLayoutManager(new GraphLayoutManager(this));
		figure.addPropertyChangeListener(((ProductSystemNode) getModel())
				.getEditor());
		return figure;
	}

	@Override
	public List<Node> getModelChildren() {
		return ((ProductSystemNode) getModel()).getChildrenArray();
	}

	@Override
	public boolean isSelectable() {
		return false;
	}

	@Override
	public void deactivate() {
		IFigure figure = getFigure();
		if (figure instanceof ProductSystemFigure) {
			ProductSystemFigure pFigure = (ProductSystemFigure) figure;
			Font infoFont = pFigure.getInfoFont();
			if (infoFont != null && !infoFont.isDisposed())
				infoFont.dispose();
		}
		super.deactivate();
	}

}
