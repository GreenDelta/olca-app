package org.openlca.app.editors.sd.editor.graph;

import org.eclipse.draw2d.IFigure;
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
	public StockModel getModel() {
		var model = super.getModel();
		return model instanceof StockModel m ? m : null;
	}

	@Override
	public void activate() {
		super.activate();
		var model = getModel();
		if (model != null) {
			model.addListener(listener);
		}
	}

	@Override
	public void deactivate() {
		var model = getModel();
		if (model != null) {
			model.removeListener(listener);
		}
		super.deactivate();
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.PRIMARY_DRAG_ROLE, new ResizableEditPolicy());
	}

	@Override
	protected void refreshVisuals() {
		var model = getModel();
		if (model == null) return;
		var figure = (StockFigure) getFigure();
		figure.setText(model.name());

		var parent = getParent();
		if (parent instanceof AbstractGraphicalEditPart gep) {
			gep.setLayoutConstraint(this, figure, model.bounds);
		}
	}

}
