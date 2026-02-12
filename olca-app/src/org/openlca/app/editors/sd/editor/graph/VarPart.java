package org.openlca.app.editors.sd.editor.graph;

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.editpolicies.ResizableEditPolicy;
import org.openlca.sd.eqn.Var;

class VarPart extends AbstractGraphicalEditPart {

	private final Runnable listener = this::refreshVisuals;

	@Override
	protected IFigure createFigure() {
		var model = getModel();
		return switch (model.variable) {
			case Var.Stock s -> new StockFigure();
			case Var.Aux a -> new AuxFigure();
			case Var.Rate r -> new FlowFigure();
			default -> new StockFigure();
		};
	}

	@Override
	public VarModel getModel() {
		var model = super.getModel();
		return model instanceof VarModel m ? m : null;
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
		var figure = getFigure();
		if (figure instanceof StockFigure f) {
			f.setText(model.name());
		} else if (figure instanceof AuxFigure f) {
			f.setText(model.name());
		} else if (figure instanceof FlowFigure f) {
			f.setText(model.name());
		}

		var parent = getParent();
		if (parent instanceof AbstractGraphicalEditPart gep) {
			gep.setLayoutConstraint(this, figure, model.bounds);
		}
	}

}
