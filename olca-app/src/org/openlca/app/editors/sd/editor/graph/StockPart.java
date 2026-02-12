package org.openlca.app.editors.sd.editor.graph;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;

class StockPart extends AbstractGraphicalEditPart {

	@Override
	protected IFigure createFigure() {
		return new StockFigure();
	}

	@Override
	protected void createEditPolicies() {
	}

	@Override
	protected void refreshVisuals() {
		var model = (StockModel) getModel();
		var figure = (StockFigure) getFigure();
		figure.setText(model.name());

		var parent = getParent();
		if (!(parent instanceof AbstractGraphicalEditPart gep))
			return;

		var bounds = new Rectangle(model.x, model.y, model.width, model.height);
		gep.setLayoutConstraint(this, figure, bounds);
	}

}
