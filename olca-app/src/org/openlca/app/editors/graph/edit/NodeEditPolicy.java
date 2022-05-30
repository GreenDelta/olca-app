package org.openlca.app.editors.graph.edit;

import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.requests.GroupRequest;
import org.openlca.app.editors.graph.model.Graph;
import org.openlca.app.editors.graph.model.Node;
import org.openlca.app.editors.graph.model.commands.ExpansionCommand;
import org.openlca.app.editors.graph.model.commands.NodeDeleteCommand;
import org.openlca.app.editors.graph.requests.ExpansionRequest;

public class NodeEditPolicy extends MinMaxComponentEditPolicy {

	@Override
	public Command getCommand(Request request) {
		if (request instanceof ExpansionRequest req) {
			return getExpansionCommand(req.getNode(), req.getSide());
		}
		return super.getCommand(request);
	}

	private Command getExpansionCommand(Node node, Node.Side side) {
		var command = new ExpansionCommand(node, side);
		command.setParent(getHost().getParent());
		return command;
	}

	@Override
	protected Command createDeleteCommand(GroupRequest req) {
		var parent = getHost().getParent().getModel();
		var child = getHost().getModel();
		if (parent instanceof Graph graph && child instanceof Node node) {
			return new NodeDeleteCommand(graph, node);
		}
		return super.createDeleteCommand(req);
	}

}
