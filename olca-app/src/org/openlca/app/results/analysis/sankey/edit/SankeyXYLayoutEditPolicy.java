package org.openlca.app.results.analysis.sankey.edit;

import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.ResizableEditPolicy;
import org.eclipse.gef.editpolicies.XYLayoutEditPolicy;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.requests.CreateRequest;
import org.openlca.app.results.analysis.sankey.SankeyConfig;
import org.openlca.app.results.analysis.sankey.model.Diagram;
import org.openlca.app.results.analysis.sankey.model.SankeyNode;
import org.openlca.app.results.analysis.sankey.model.commands.EditConfigCommand;
import org.openlca.app.tools.graphics.model.commands.ComponentSetConstraintCommand;
import org.openlca.app.tools.graphics.model.commands.LayoutCommand;

import static org.openlca.app.editors.graphical.requests.GraphRequestConstants.REQ_LAYOUT;
import static org.openlca.app.results.analysis.sankey.SankeyConfig.CONFIG_PROP;
import static org.openlca.app.results.analysis.sankey.requests.SankeyRequestConstants.REQ_EDIT_CONFIG;

public class SankeyXYLayoutEditPolicy extends XYLayoutEditPolicy {

	@Override
	public Command getCommand(Request request) {
		if (REQ_LAYOUT.equals(request.getType()))
			return new LayoutCommand((DiagramEditPart) getHost());
		if (REQ_EDIT_CONFIG.equals(request.getType()))
			return getEditConfigCommand(request);
		return super.getCommand(request);
	}

	@Override
	protected Command getCreateCommand(CreateRequest request) {
		return null;
	}

	private Command getEditConfigCommand(Request request) {
		var newConfig = (SankeyConfig) request.getExtendedData().get(CONFIG_PROP);
		return new EditConfigCommand((Diagram) getHost().getModel(), newConfig);
	}


	@Override
	protected EditPolicy createChildEditPolicy(EditPart child) {
		var policy = new ResizableEditPolicy();
		policy.setResizeDirections(
				PositionConstants.EAST | PositionConstants.WEST);
		return policy;
	}


	@Override
	protected Command createChangeConstraintCommand(
			ChangeBoundsRequest request, EditPart child, Object constraint) {
		if (child instanceof SankeyNodeEditPart
				&& constraint instanceof Rectangle) {
			return new ComponentSetConstraintCommand((SankeyNode) child.getModel(),
					request, (Rectangle) constraint);
		}
		return super.createChangeConstraintCommand(request, child,
				constraint);
	}

}
