package org.openlca.app.editors.graphical.model;

import java.util.List;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.LineBorder;
import org.openlca.app.editors.graphical.layout.LayoutManager;
import org.openlca.app.editors.graphical.layout.LayoutType;
import org.openlca.app.editors.graphical.layout.NodeLayoutStore;
import org.openlca.app.editors.graphical.layout.NodeLayoutStore.NodeLayoutException;

class ProductSystemFigure extends Figure {

	private boolean firstTime = true;
	private ProductSystemNode node;

	ProductSystemFigure(ProductSystemNode node) {
		setForegroundColor(ColorConstants.black);
		setBorder(new LineBorder(1));
		node.figure = this;
		this.node = node;
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
	public void paint(Graphics graphics) {
		super.paint(graphics);
		if (!firstTime)
			return;
		firstTime = false;
		boolean layoutLoaded = false;
		if (!node.editor.isInitialized()) {
			node.editor.setInitialized(true);
			try {
				layoutLoaded = NodeLayoutStore.loadLayout(node);
			} catch (NodeLayoutException e) {
				layoutLoaded = false;
			}
		}
		if (layoutLoaded)
			return;
		long refId = node.getProductSystem().referenceProcess.id;
		ProcessNode refNode = node.getProcessNode(refId);
		refNode.expandLeft();
		refNode.expandRight();
		getLayoutManager().layout(this, LayoutType.TREE_LAYOUT);
	}
}
