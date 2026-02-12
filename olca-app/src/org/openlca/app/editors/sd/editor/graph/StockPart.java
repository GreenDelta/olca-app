package org.openlca.app.editors.sd.editor.graph;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.editpolicies.ResizableEditPolicy;

class StockPart extends AbstractGraphicalEditPart {

	private final Runnable listener = this::refreshVisuals;

	@Override
	protected IFigure createFigure() {
		return new StockFigure();
	}

	@Override
	public void activate() {
		super.activate();
		((StockModel) getModel()).addListener(listener);
	}

	@Override
	public void deactivate() {
		((StockModel) getModel()).removeListener(listener);
		super.deactivate();
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.PRIMARY_DRAG_ROLE, new ResizableEditPolicy());
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
