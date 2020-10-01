package org.openlca.app.results.analysis.sankey.model;

import java.util.List;

import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.openlca.app.results.analysis.sankey.layout.GraphLayoutManager;
import org.openlca.app.results.analysis.sankey.layout.LayoutPolicy;

public class ProductSystemPart extends AbstractGraphicalEditPart {

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.LAYOUT_ROLE, new LayoutPolicy());
	}

	@Override
	protected ProductSystemFigure createFigure() {
		var figure = new ProductSystemFigure((ProductSystemNode) getModel());
		figure.setLayoutManager(new GraphLayoutManager(this));
		return figure;
	}

	@Override
	public List<ProcessNode> getModelChildren() {
		return ((ProductSystemNode) getModel()).processNodes;
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
