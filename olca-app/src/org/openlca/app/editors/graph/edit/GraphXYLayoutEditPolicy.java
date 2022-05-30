package org.openlca.app.editors.graph.edit;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.editpolicies.XYLayoutEditPolicy;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.osgi.util.NLS;
import org.openlca.app.M;
import org.openlca.app.editors.graph.layouts.TreeLayoutProcessor;
import org.openlca.app.editors.graph.model.Node;
import org.openlca.app.editors.graph.model.commands.NodeSetConstraintCommand;
import org.openlca.app.editors.graph.requests.ExpansionRequest;

import static org.openlca.app.editors.graph.actions.LayoutAction.REQ_LAYOUT;
import static org.openlca.app.editors.graph.requests.ExpansionRequest.REQ_EXPANSION;

public class GraphXYLayoutEditPolicy extends XYLayoutEditPolicy {

	@Override
	public Command getCommand(Request request) {
		if (REQ_LAYOUT.equals(request.getType()))
			return getLayoutCommand();
		return super.getCommand(request);
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

	protected Command getLayoutCommand() {
		var graphEditPart = (GraphEditPart) getHost();
		var graph = graphEditPart.getModel();

		CompoundCommand cc = new CompoundCommand();
		cc.setLabel(NLS.bind(M.LayoutAs, M.Tree));

		var layoutProcessor = new TreeLayoutProcessor(graph);
		var mapNodeToMoveDelta = layoutProcessor.getMoveDeltas();

		for (NodeEditPart part : graphEditPart.getChildren()) {
			var request = new ChangeBoundsRequest(REQ_MOVE_CHILDREN);
			request.setEditParts(part);
			request.setMoveDelta(mapNodeToMoveDelta.get(part.getModel()));
			cc.add(getCommand(request));
		}
		return cc;
	}

	@Override
	protected Command getCreateCommand(CreateRequest request) {
		return null;

		// TODO Create command
		//			@Override
		//			protected Command getCreateCommand(CreateRequest request) {
		//				Object childClass = request.getNewObjectType();
		//				if (childClass == EllipticalShape.class
		//					|| childClass == RectangularShape.class) {
		//					// return a command that can add a Shape to a ShapesDiagram
		//					return new ShapeCreateCommand((Shape) request.getNewObject(),
		//						(ShapesDiagram) getHost().getModel(),
		//						(Rectangle) getConstraintFor(request));
		//				}
		//				return null;
		//			}
	}

}
