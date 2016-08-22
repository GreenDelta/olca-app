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
import org.openlca.app.editors.graphical.command.CommandFactory;
import org.openlca.app.editors.graphical.command.CreateLinkCommand;
import org.openlca.app.editors.graphical.model.ConnectionLink;
import org.openlca.app.editors.graphical.model.ExchangeNode;
import org.openlca.app.editors.graphical.model.ProcessNode;

public class ProcessLinkCreatePolicy extends GraphicalNodeEditPolicy {

	private PolylineConnection connection;

	@Override
	protected Connection createDummyConnection(Request req) {
		connection = (PolylineConnection) super.createDummyConnection(req);
		connection.setForegroundColor(ConnectionLink.COLOR);
		if (req instanceof CreateConnectionRequest) {
			CreateLinkCommand command = (CreateLinkCommand) ((CreateConnectionRequest) req)
					.getStartCommand();
			if (command.getSourceNode() != null)
				connection.setTargetDecoration(new PolygonDecoration());
			else if (command.getTargetNode() != null)
				connection.setSourceDecoration(new PolygonDecoration());
		} else
			connection.setTargetDecoration(new PolygonDecoration());
		return connection;
	}

	@Override
	protected Command getConnectionCompleteCommand(
			CreateConnectionRequest request) {
		if (request.getStartCommand() != null) {
			CreateLinkCommand cmd = (CreateLinkCommand) request
					.getStartCommand();
			if (request.getTargetEditPart().getModel() instanceof ExchangeNode) {
				ExchangeNode target = (ExchangeNode) request
						.getTargetEditPart().getModel();
				ProcessNode targetNode = target.getParent().getParent();
				if (!target.getExchange().isInput())
					cmd.setSourceNode(targetNode);
				else if (!targetNode.hasIncomingConnection(cmd.getFlowId()))
					cmd.setTargetNode(targetNode);
				request.setStartCommand(cmd);
				return cmd;
			}
		}
		return null;
	}

	@Override
	protected Command getConnectionCreateCommand(
			final CreateConnectionRequest request) {
		CreateLinkCommand cmd = null;
		ExchangeNode target = (ExchangeNode) request.getTargetEditPart()
				.getModel();
		ProcessNode targetNode = target.getParent().getParent();
		long flowId = target.getExchange().getFlow().getId();
		if (!target.getExchange().isInput()) {
			cmd = CommandFactory.createCreateLinkCommand(flowId);
			cmd.setSourceNode(targetNode);
			cmd.setStartedFromSource(true);
			request.setStartCommand(cmd);
		} else if (!targetNode.hasIncomingConnection(flowId)) {
			cmd = CommandFactory.createCreateLinkCommand(flowId);
			cmd.setTargetNode(targetNode);
			cmd.setStartedFromSource(false);
			request.setStartCommand(cmd);
		}
		return cmd;
	}

	@Override
	protected ConnectionRouter getDummyConnectionRouter(
			CreateConnectionRequest request) {
		return ConnectionRouter.NULL;
	}

	@Override
	protected Command getReconnectSourceCommand(ReconnectRequest request) {
		if (request.getTarget().getModel() instanceof ExchangeNode) {
			ConnectionLink link = (ConnectionLink) request
					.getConnectionEditPart().getModel();
			ExchangeNode source = (ExchangeNode) request.getTarget().getModel();
			ProcessNode sourceNode = source.getParent().getParent();
			return CommandFactory.createReconnectLinkCommand(link, sourceNode,
					link.getTargetNode());
		}
		return null;
	}

	@Override
	protected Command getReconnectTargetCommand(ReconnectRequest request) {
		if (request.getTarget().getModel() instanceof ExchangeNode) {
			ConnectionLink link = (ConnectionLink) request
					.getConnectionEditPart().getModel();
			ExchangeNode target = (ExchangeNode) request.getTarget().getModel();
			ProcessNode targetNode = target.getParent().getParent();
			long flowId = link.getProcessLink().flowId;
			boolean canConnect = true;
			if (!link.getTargetNode().equals(targetNode)
					&& targetNode.hasIncomingConnection(flowId))
				canConnect = false;
			if (canConnect)
				return CommandFactory.createReconnectLinkCommand(link,
						link.getSourceNode(), targetNode);
		}
		return null;
	}

	@Override
	public void eraseSourceFeedback(Request request) {
		if (getHost().getModel() instanceof ExchangeNode) {
			ExchangeNode node = (ExchangeNode) getHost().getModel();
			node.getParent().getParent().getParent().removeHighlighting();
			node.setHighlighted(false);
		}
		super.eraseSourceFeedback(request);
	}

	@Override
	public void showSourceFeedback(Request request) {
		if (getHost().getModel() instanceof ExchangeNode) {
			ExchangeNode node = (ExchangeNode) getHost().getModel();
			node.getParent().getParent().getParent()
					.highlightMatchingExchanges(node);
			node.setHighlighted(true);
		}
		super.showSourceFeedback(request);
	}
}
