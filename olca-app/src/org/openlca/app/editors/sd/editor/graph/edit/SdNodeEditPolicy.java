package org.openlca.app.editors.sd.editor.graph.edit;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.ComponentEditPolicy;
import org.eclipse.gef.requests.GroupRequest;
import org.openlca.app.editors.sd.editor.graph.model.SdNode;
import org.openlca.app.editors.sd.editor.graph.model.commands.DeleteNodeCommand;

/**
 * Edit policy for SdNode that handles deletion.
 */
public class SdNodeEditPolicy extends ComponentEditPolicy {

	@Override
	protected Command createDeleteCommand(GroupRequest deleteRequest) {
		var node = (SdNode) getHost().getModel();
		var graph = node.getGraph();
		if (graph != null) {
			return new DeleteNodeCommand(graph, node);
		}
		return null;
	}
}
