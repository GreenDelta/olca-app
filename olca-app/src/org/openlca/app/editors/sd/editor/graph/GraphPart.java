package org.openlca.app.editors.sd.editor.graph;

import java.util.Collections;
import java.util.List;

import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.FreeformLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.editpolicies.XYLayoutEditPolicy;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.requests.CreateRequest;
import org.openlca.app.components.graphics.themes.Theme;

class GraphPart extends AbstractGraphicalEditPart {

	private final Theme theme;

	GraphPart(GraphModel model, Theme theme) {
		setModel(model);
		this.theme = theme;
	}

	@Override
	public GraphModel getModel() {
		return super.getModel() instanceof GraphModel g ? g : null;
	}

	@Override
	protected IFigure createFigure() {
		var figure = new FreeformLayer();
		figure.setLayoutManager(new FreeformLayout());
		figure.setBackgroundColor(theme.backgroundColor());
		figure.setOpaque(true);
		return figure;
	}

	@Override
	protected void refreshVisuals() {
		super.refreshVisuals();
		var figure = getFigure();
		if (figure != null) {
			figure.setBackgroundColor(theme.backgroundColor());
		}
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.LAYOUT_ROLE, new LayoutPolicy());
	}

	@Override
	protected List<?> getModelChildren() {
		var model = getModel();
		if (model == null)
			return Collections.emptyList();
		return model.vars;
	}

	private static class LayoutPolicy extends XYLayoutEditPolicy {
		@Override
		protected Command getCreateCommand(CreateRequest request) {
			return null;
		}

		@Override
		protected Command createChangeConstraintCommand(
			ChangeBoundsRequest request, EditPart child, Object constraint) {
			if (child instanceof VarPart part && constraint instanceof Rectangle rect) {
				return new VarMoveCmd(part.getModel(), rect);
			}
			return super.createChangeConstraintCommand(request, child, constraint);
		}
	}

}
