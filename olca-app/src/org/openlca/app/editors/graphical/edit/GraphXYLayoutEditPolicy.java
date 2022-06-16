package org.openlca.app.editors.graphical.edit;

import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.editpolicies.ResizableEditPolicy;
import org.eclipse.gef.editpolicies.XYLayoutEditPolicy;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.osgi.util.NLS;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.layouts.GraphFreeformLayout;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.editors.graphical.model.commands.NodeSetConstraintCommand;

import static org.openlca.app.editors.graphical.requests.GraphRequestConstants.REQ_LAYOUT;

public class GraphXYLayoutEditPolicy extends XYLayoutEditPolicy {

	@Override
	public Command getCommand(Request request) {
		if (REQ_LAYOUT.equals(request.getType()))
			return getLayoutCommand();
		return super.getCommand(request);
	}

	@Override
	protected Command getCreateCommand(CreateRequest request) {
		return null;
	}

	@Override
	protected Command createChangeConstraintCommand(
		ChangeBoundsRequest request, EditPart child, Object constraint) {
		if (child instanceof NodeEditPart && constraint instanceof Rectangle) {
			return new NodeSetConstraintCommand((Node) child.getModel(),
				request, (Rectangle) constraint);
		}
		return super.createChangeConstraintCommand(request, child,
			constraint);
	}

	@Override
	protected EditPolicy createChildEditPolicy(EditPart child) {
		var policy = new ResizableEditPolicy();
		policy.setResizeDirections(
			PositionConstants.EAST | PositionConstants.WEST);
		return policy;
	}

	protected Command getLayoutCommand() {
		var graphEditPart = (GraphEditPart) getHost();
		var layoutManager = (GraphFreeformLayout) graphEditPart.getFigure()
			.getLayoutManager();

		CompoundCommand cc = new CompoundCommand();
		cc.setLabel(NLS.bind(M.LayoutAs.toLowerCase(), M.Tree));

		var mapNodeToLocation = layoutManager.layoutProcessor.getNodeLocations();

		for (NodeEditPart part : graphEditPart.getChildren()) {
			var request = new ChangeBoundsRequest(REQ_MOVE_CHILDREN);
			request.setEditParts(part);
			var newLoc = mapNodeToLocation.get(part.getFigure());
			var oldLoc = part.getFigure().getLocation();
			var moveDelta = newLoc.getTranslated(oldLoc.getNegated());
			request.setMoveDelta(moveDelta);
			cc.add(getCommand(request));
		}
		return cc.unwrap();
	}

}
