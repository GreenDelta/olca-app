package org.openlca.app.editors.sd.editor.graph;

import java.util.List;

import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.editpolicies.ResizableEditPolicy;
import org.openlca.sd.eqn.Var.Aux;
import org.openlca.sd.eqn.Var.Rate;
import org.openlca.sd.eqn.Var.Stock;

class VarPart extends AbstractGraphicalEditPart implements NodeEditPart {

	private final Runnable listener = () -> {
		refreshVisuals();
		refreshSourceConnections();
		refreshTargetConnections();
	};

	@Override
	protected IFigure createFigure() {
		var variable = getModel().variable;
		if (variable instanceof Stock) {
			return new StockFigure();
		}
		if (variable instanceof Aux) {
			return new AuxFigure();
		}
		if (variable instanceof Rate) {
			return new FlowFigure();
		}
		return new StockFigure();
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
	public ConnectionAnchor getSourceConnectionAnchor(ConnectionEditPart connection) {
		return new ChopboxAnchor(getFigure());
	}

	@Override
	public ConnectionAnchor getTargetConnectionAnchor(ConnectionEditPart connection) {
		return new ChopboxAnchor(getFigure());
	}

	@Override
	public ConnectionAnchor getSourceConnectionAnchor(Request request) {
		return new ChopboxAnchor(getFigure());
	}

	@Override
	public ConnectionAnchor getTargetConnectionAnchor(Request request) {
		return new ChopboxAnchor(getFigure());
	}

	@Override
	protected List<LinkModel> getModelSourceConnections() {
		var model = getModel();
		return model != null ? model.sourceLinks : List.of();
	}

	@Override
	protected List<LinkModel> getModelTargetConnections() {
		var model = getModel();
		return model != null ? model.targetLinks : List.of();
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
