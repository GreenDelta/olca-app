package org.openlca.app.editors.graphical_legacy.view;

import java.util.List;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.app.editors.graphical_legacy.layout.LayoutManager;
import org.openlca.app.editors.graphical_legacy.model.ProductSystemNode;

public class ProductSystemFigure extends Figure {

	private final ProductSystemNode node;
	private final LineBorder border;

	public ProductSystemFigure(ProductSystemNode node) {
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
		border.setColor(theme.graphBackgroundColor());
		g.pushState();
		g.setBackgroundColor(theme.graphBackgroundColor());
		g.fillRectangle(new Rectangle(getLocation(), getSize()));
		g.popState();
		super.paint(g);
	}

}
