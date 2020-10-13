package org.openlca.app.editors.graphical.model;

import java.util.List;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.LineBorder;
import org.openlca.app.editors.graphical.layout.LayoutManager;

class ProductSystemFigure extends Figure {

	ProductSystemFigure(ProductSystemNode node) {
		setForegroundColor(ColorConstants.black);
		setBorder(new LineBorder(1));
		node.figure = this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<ProcessFigure> getChildren() {
		return super.getChildren();
	}

	@Override
	public LayoutManager getLayoutManager() {
		return (LayoutManager) super.getLayoutManager();
	}

}
