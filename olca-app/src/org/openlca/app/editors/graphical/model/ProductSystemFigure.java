package org.openlca.app.editors.graphical.model;

import java.util.List;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.LineBorder;
import org.openlca.app.editors.graphical.layout.GraphLayoutManager;
import org.openlca.app.editors.graphical.layout.GraphLayoutType;
import org.openlca.app.editors.graphical.layout.NodeLayoutStore;

class ProductSystemFigure extends Figure {

	private boolean firstTime = true;
	private ProductSystemNode node;

	ProductSystemFigure(ProductSystemNode node) {
		setForegroundColor(ColorConstants.black);
		setBorder(new LineBorder(1));
		node.setFigure(this);
		this.node = node;
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
		super.paint(graphics);
		if (!firstTime)
			return;
		firstTime = false;
		boolean layoutLoaded = false;
		if (!node.getEditor().isInitialized()) {
			node.getEditor().setInitialized(true);
			layoutLoaded = NodeLayoutStore.loadLayout(node);
		}
		if (layoutLoaded)
			return;
		long refId = node.getProductSystem().getReferenceProcess().getId();
		node.getProcessNode(refId).expandLeft();
		getLayoutManager().layout(this, GraphLayoutType.TREE_LAYOUT);
	}

}
