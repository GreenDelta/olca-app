package org.openlca.app.results.analysis.sankey.model;

import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.swt.graphics.Font;
import org.openlca.app.results.analysis.sankey.layout.GraphLayoutManager;
import org.openlca.app.results.analysis.sankey.layout.LayoutPolicy;

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
