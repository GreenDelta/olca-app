package org.openlca.app.results.analysis.sankey.model;

import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.swt.graphics.Font;
import org.openlca.app.results.analysis.sankey.layout.GraphLayoutManager;
import org.openlca.app.results.analysis.sankey.layout.LayoutPolicy;

public class ProductSystemPart extends AbstractGraphicalEditPart {

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.LAYOUT_ROLE, new LayoutPolicy());
	}

	@Override
	protected IFigure createFigure() {
		var figure = new ProductSystemFigure((ProductSystemNode) getModel());
		figure.setLayoutManager(new GraphLayoutManager(this));
		figure.addPropertyChangeListener(((ProductSystemNode) getModel()).editor);
		return figure;
	}

	@Override
	public List<Node> getModelChildren() {
		return ((ProductSystemNode) getModel()).children;
	}

	@Override
	public boolean isSelectable() {
		return false;
	}

	@Override
	public void deactivate() {
		var figure = getFigure();
		if (figure instanceof ProductSystemFigure) {
			var pFigure = (ProductSystemFigure) figure;
			var infoFont = pFigure.infoFont;
			if (infoFont != null && !infoFont.isDisposed())
				infoFont.dispose();
		}
		super.deactivate();
	}

}
