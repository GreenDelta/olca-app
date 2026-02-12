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

		// for now, use a default position and size
		var parent = getParent();
		if (!(parent instanceof AbstractGraphicalEditPart gep))
			return;

		int index = gep.getChildren().indexOf(this);
		var bounds = new Rectangle(10 + index * 10, 10 + index * 10, 100, 50);
		gep.setLayoutConstraint(this, figure, bounds);
	}

}
