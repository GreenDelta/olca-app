package org.openlca.app.editors.sd.editor.graph.edit;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.XYLayoutEditPolicy;
import org.eclipse.gef.requests.CreateRequest;
import org.openlca.app.editors.sd.editor.graph.model.SdGraph;
import org.openlca.app.editors.sd.editor.graph.model.SdNode;
import org.openlca.app.editors.sd.editor.graph.model.commands.CreateNodeCommand;
import org.openlca.app.editors.sd.editor.graph.model.commands.MoveNodeCommand;

/**
 * Edit policy for the SdGraph that handles node creation and movement.
 */
public class SdGraphXYLayoutEditPolicy extends XYLayoutEditPolicy {

	@Override
	protected Command createChangeConstraintCommand(EditPart child, Object constraint) {
		if (child.getModel() instanceof SdNode node && constraint instanceof Rectangle rect) {
			return new MoveNodeCommand(node, rect);
		}
		return null;
	}

	@Override
	protected Command getCreateCommand(CreateRequest request) {
		if (request.getNewObject() instanceof SdNode newNode) {
			var graph = (SdGraph) getHost().getModel();
			var constraint = (Rectangle) getConstraintFor(request);
			return new CreateNodeCommand(graph, newNode, constraint);
		}
		return null;
	}
}
