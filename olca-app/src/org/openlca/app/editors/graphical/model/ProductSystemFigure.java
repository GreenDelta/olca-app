package org.openlca.app.editors.graphical.model;

import java.util.List;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.app.editors.graphical.layout.LayoutManager;

class ProductSystemFigure extends Figure {

	private final ProductSystemNode node;
	private final LineBorder border;

	ProductSystemFigure(ProductSystemNode node) {
		this.node = node;
		node.figure = this;
		border = new LineBorder(1);
		setBorder(border);
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

	@Override
	public void paint(Graphics g) {
		var theme = node.config().theme();
		border.setColor(theme.graphBackground());
		g.pushState();
		g.setBackgroundColor(theme.graphBackground());
		g.fillRectangle(new Rectangle(getLocation(), getSize()));
		g.popState();
		super.paint(g);
	}

}
