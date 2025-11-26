package org.openlca.app.editors.sd.editor.graph.edit;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.GraphicalNodeEditPolicy;
import org.eclipse.gef.requests.CreateConnectionRequest;
import org.eclipse.gef.requests.ReconnectRequest;
import org.openlca.app.editors.sd.editor.graph.model.SdNode;
import org.openlca.app.editors.sd.editor.graph.model.commands.CreateLinkCommand;

/**
 * Edit policy for creating connections between nodes.
 */
public class SdNodeGraphicalEditPolicy extends GraphicalNodeEditPolicy {

	@Override
	protected Command getConnectionCreateCommand(CreateConnectionRequest request) {
		var source = (SdNode) getHost().getModel();
		var cmd = new CreateLinkCommand(source);
		request.setStartCommand(cmd);
		return cmd;
	}

	@Override
	protected Command getConnectionCompleteCommand(CreateConnectionRequest request) {
		var cmd = (CreateLinkCommand) request.getStartCommand();
		var target = (SdNode) getHost().getModel();
		cmd.setTarget(target);
		return cmd;
	}

	@Override
	protected Command getReconnectSourceCommand(ReconnectRequest request) {
		// TODO: Implement reconnection if needed
		return null;
	}

	@Override
	protected Command getReconnectTargetCommand(ReconnectRequest request) {
		// TODO: Implement reconnection if needed
		return null;
	}
}
