package org.openlca.app.editors.graphical.policy;

import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.ConnectionRouter;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.GraphicalNodeEditPolicy;
import org.eclipse.gef.requests.CreateConnectionRequest;
import org.eclipse.gef.requests.ReconnectRequest;
import org.openlca.app.editors.graphical.command.CreateLinkCommand;
import org.openlca.app.editors.graphical.command.ReconnectLinkCommand;
import org.openlca.app.editors.graphical.model.ExchangeNode;
import org.openlca.app.editors.graphical.model.Link;
import org.openlca.app.editors.graphical.model.ProductSystemNode;

public class ProcessLinkCreatePolicy extends GraphicalNodeEditPolicy {

	@Override
	protected Connection createDummyConnection(Request req) {
		PolylineConnection connection = (PolylineConnection) super.createDummyConnection(req);
		connection.setForegroundColor(Link.COLOR);
		if (!(req instanceof CreateConnectionRequest)) {
			connection.setTargetDecoration(new PolygonDecoration());
			return connection;
		}
		CreateLinkCommand command = (CreateLinkCommand) ((CreateConnectionRequest) req).getStartCommand();
		if (command.sourceNode != null)
			connection.setTargetDecoration(new PolygonDecoration());
		else if (command.targetNode != null)
			connection.setSourceDecoration(new PolygonDecoration());
		return connection;
	}

	@Override
	protected ConnectionRouter getDummyConnectionRouter(CreateConnectionRequest request) {
		return ConnectionRouter.NULL;
	}

	@Override
	protected Command getConnectionCompleteCommand(CreateConnectionRequest request) {
		CreateLinkCommand cmd = (CreateLinkCommand) request.getStartCommand();
		if (cmd == null)
			return null;
		ExchangeNode toConnect = getNode(request);
		ExchangeNode other = cmd.startedFromSource ? cmd.sourceNode.getOutput(cmd.flowId) : cmd.targetNode;
		if (!toConnect.matches(other))
			return null;
		if (cmd.startedFromSource)
			cmd.targetNode = toConnect;
		else if (!toConnect.parent().hasIncoming(toConnect.exchange.getId()))
			cmd.sourceNode = toConnect.parent();
		if (cmd.sourceNode == null || cmd.targetNode == null)
			return null;
		request.setStartCommand(cmd);
		return cmd;
	}

	@Override
	protected Command getConnectionCreateCommand(CreateConnectionRequest request) {
		ExchangeNode toConnect = getNode(request);
		long flowId = toConnect.exchange.getFlow().getId();
		if (!toConnect.exchange.isInput()) {
			CreateLinkCommand cmd = new CreateLinkCommand(flowId);
			cmd.sourceNode = toConnect.parent();
			cmd.startedFromSource = true;
			request.setStartCommand(cmd);
			return cmd;
		} else if (!toConnect.parent().hasIncoming(toConnect.exchange.getId())) {
			CreateLinkCommand cmd = new CreateLinkCommand(flowId);
			cmd.targetNode = toConnect;
			cmd.startedFromSource = false;
			request.setStartCommand(cmd);
			return cmd;
		}
		return null;
	}

	private ExchangeNode getNode(CreateConnectionRequest request) {
		return (ExchangeNode) request.getTargetEditPart().getModel();
	}

	@Override
	protected Command getReconnectSourceCommand(ReconnectRequest request) {
		Link link = getLink(request);
		ExchangeNode toConnect = getNode(request);
		ExchangeNode other = link.targetNode.getNode(link.processLink.exchangeId);
		if (!toConnect.matches(other))
			return null;
		return new ReconnectLinkCommand(toConnect.parent(), other, link);
	}

	@Override
	protected Command getReconnectTargetCommand(ReconnectRequest request) {
		Link link = getLink(request);
		ExchangeNode toConnect = getNode(request);
		ExchangeNode other = link.sourceNode.getOutput(link.processLink.flowId);
		if (!toConnect.matches(other))
			return null;
		boolean sameNode = toConnect.exchange.getId() == link.processLink.exchangeId;
		if (!sameNode && toConnect.parent().hasIncoming(toConnect.exchange.getId()))
			return null;
		return new ReconnectLinkCommand(link.sourceNode, toConnect, link);
	}

	private Link getLink(ReconnectRequest request) {
		return (Link) request.getConnectionEditPart().getModel();
	}

	private ExchangeNode getNode(ReconnectRequest request) {
		return (ExchangeNode) request.getTarget().getModel();
	}

	@Override
	public void eraseSourceFeedback(Request request) {
		ExchangeNode node = (ExchangeNode) getHost().getModel();
		ProductSystemNode psNode = node.parent().parent();
		psNode.removeHighlighting();
		node.setHighlighted(false);
	}

	@Override
	public void showSourceFeedback(Request request) {
		ExchangeNode node = (ExchangeNode) getHost().getModel();
		ProductSystemNode psNode = node.parent().parent();
		psNode.highlightMatchingExchanges(node);
		node.setHighlighted(true);
	}
}
