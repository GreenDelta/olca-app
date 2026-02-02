package org.openlca.app.editors.graphical.edit;

import static org.openlca.app.editors.graphical.actions.RemoveChainAction.*;
import static org.openlca.app.editors.graphical.requests.GraphRequests.*;

import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.RootComponentEditPolicy;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.GraphLink;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.editors.graphical.model.commands.RemoveChainCommand;

public class GraphEditPolicy extends RootComponentEditPolicy {

	@Override
	public Command getCommand(Request request) {
		return REQ_REMOVE_CHAIN.equals(request.getType())
			? getRemoveSupplyChainCommand(request)
			: super.getCommand(request);
	}

	private Command getRemoveSupplyChainCommand(Request req) {
		var root = req.getExtendedData().get(KEY_ROOT);
		var graph = (Graph) getHost().getModel();
		if (root instanceof Node node) {
			return new RemoveChainCommand(graph, node);
		}
		if (root instanceof GraphLink link) {
			return new RemoveChainCommand(graph, providerNodeOf(link));
		}
		return null;
	}

	private Node providerNodeOf(GraphLink link) {
		if (link == null) return null;
		var s = link.getSourceNode();
		if (s != null
			&& s.descriptor != null
			&& s.descriptor.id == link.processLink.providerId) {
			return s;
		}
		var t = link.getTargetNode();
		if (t != null
			&& t.descriptor != null
			&& t.descriptor.id == link.processLink.providerId) {
			return t;
		}
		return null;
	}
}
