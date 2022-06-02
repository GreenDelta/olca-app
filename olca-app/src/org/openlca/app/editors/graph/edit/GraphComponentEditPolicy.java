package org.openlca.app.editors.graph.edit;

import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.ComponentEditPolicy;
import org.eclipse.gef.requests.GroupRequest;
import org.openlca.app.editors.graph.model.ExchangeItem;
import org.openlca.app.editors.graph.model.Graph;
import org.openlca.app.editors.graph.model.IOPane;
import org.openlca.app.editors.graph.model.Node;
import org.openlca.app.editors.graph.model.commands.DeleteExchangeCommand;
import org.openlca.app.editors.graph.model.commands.DeleteNodeCommand;
import org.openlca.app.editors.graph.model.commands.EditExchangeCommand;

import static org.openlca.app.editors.graph.requests.GraphRequestConstants.REQ_EDIT;

public class GraphComponentEditPolicy extends ComponentEditPolicy {

	@Override
	public Command getCommand(Request request) {
		var child = getHost().getModel();
		if (REQ_EDIT.equals(request.getType())) {
			if (child instanceof ExchangeItem exchangeItem)
				return getEditExchangeEditCommand(exchangeItem);
		}
		return super.getCommand(request);
	}

	@Override
	protected Command createDeleteCommand(GroupRequest req) {
		var parent = getHost().getParent().getModel();
		var child = getHost().getModel();
		if (parent instanceof Graph graph && child instanceof Node node)
			return new DeleteNodeCommand(graph, node);
		if (parent instanceof IOPane pane
			&& child instanceof ExchangeItem exchangeItem
			&& pane.getNode().isEditable())
			return new DeleteExchangeCommand(pane, exchangeItem);
		return super.createDeleteCommand(req);
	}

	protected Command getEditExchangeEditCommand(ExchangeItem exchangeItem) {
		return new EditExchangeCommand(exchangeItem);
	}

}
