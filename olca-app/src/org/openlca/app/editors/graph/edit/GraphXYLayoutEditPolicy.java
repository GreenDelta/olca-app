package org.openlca.app.editors.graph.edit;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.XYLayoutEditPolicy;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.requests.CreateRequest;
import org.openlca.app.editors.graph.model.Node;
import org.openlca.app.editors.graph.model.commands.NodeSetConstraintCommand;

public class GraphXYLayoutEditPolicy extends XYLayoutEditPolicy {

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
