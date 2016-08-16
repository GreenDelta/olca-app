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
import org.openlca.app.editors.graphical.GraphUtil;
import org.openlca.app.editors.graphical.command.CommandFactory;
import org.openlca.app.editors.graphical.command.CreateLinkCommand;
import org.openlca.app.editors.graphical.model.ConnectionLink;
import org.openlca.app.editors.graphical.model.ExchangeNode;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.core.model.Exchange;

public class LinkPolicy extends GraphicalNodeEditPolicy {

	private PolylineConnection connection;

	@Override
	protected Connection createDummyConnection(Request req) {
		connection = (PolylineConnection) super.createDummyConnection(req);
		connection.setForegroundColor(ConnectionLink.COLOR);
		if (req instanceof CreateConnectionRequest) {
			CreateLinkCommand command = (CreateLinkCommand) ((CreateConnectionRequest) req)
					.getStartCommand();
			if (command.providerNode != null)
				connection.setTargetDecoration(new PolygonDecoration());
			else if (command.exchangeNode != null)
				connection.setSourceDecoration(new PolygonDecoration());
		} else
			connection.setTargetDecoration(new PolygonDecoration());
		return connection;
	}

	@Override
	protected Command getConnectionCompleteCommand(CreateConnectionRequest req) {
		if (req.getStartCommand() == null)
			return null;
		CreateLinkCommand cmd = (CreateLinkCommand) req.getStartCommand();
		Object model = req.getTargetEditPart().getModel();
		if (!(model instanceof ExchangeNode))
			return null;
		ExchangeNode node = (ExchangeNode) model;
		Exchange exchange = node.getExchange();
		if (!exchange.isInput()) {
			cmd.providerNode = node;
		} else {
			ProcessNode p = GraphUtil.getProcessNode(node);
			if (p.isLinkedExchange(exchange.getId())) {
				cmd.exchangeNode = node;
			}
		}
		return cmd;
	}

	@Override
	protected Command getConnectionCreateCommand(CreateConnectionRequest req) {
		ExchangeNode node = (ExchangeNode) req.getTargetEditPart().getModel();
		Exchange exchange = node.getExchange();
		CreateLinkCommand cmd = new CreateLinkCommand();
		if (!exchange.isInput()) {
			cmd.providerNode = node;
			cmd.startedFromSource = true;
		} else {
			ProcessNode p = GraphUtil.getProcessNode(node);
			if (!p.isLinkedExchange(exchange.getId())) {
				cmd.exchangeNode = node;
				cmd.startedFromSource = false;
			}
		}
		req.setStartCommand(cmd);
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
					link.targetNode);
		}
		return null;
	}

	@Override
	protected Command getReconnectTargetCommand(ReconnectRequest request) {
		Object model = request.getTarget().getModel();
		if (!(model instanceof ExchangeNode))
			return null;
		ConnectionLink link = (ConnectionLink) request
				.getConnectionEditPart().getModel();
		ExchangeNode node = (ExchangeNode) request.getTarget().getModel();
		ProcessNode p = GraphUtil.getProcessNode(node);
		long exchangeId = node.getExchange().getId();
		boolean canConnect = true;
		if (!link.targetNode.equals(p) && p.isLinkedExchange(exchangeId))
			canConnect = false;
		if (canConnect)
			return CommandFactory.createReconnectLinkCommand(link, link.sourceNode, p);
		return null;
	}

	@Override
	public void showSourceFeedback(Request request) {
		Object model = getHost().getModel();
		if (model instanceof ExchangeNode) {
			ExchangeNode node = (ExchangeNode) model;
			ProductSystemNode system = GraphUtil.getSystemNode(node);
			system.highlightMatchingExchanges(node);
			node.setHighlighted(true);
		}
		super.showSourceFeedback(request);
	}

	@Override
	public void eraseSourceFeedback(Request request) {
		Object model = getHost().getModel();
		if (model instanceof ExchangeNode) {
			ExchangeNode node = (ExchangeNode) model;
			ProductSystemNode system = GraphUtil.getSystemNode(node);
			system.removeHighlighting();
			node.setHighlighted(false);
		}
		super.eraseSourceFeedback(request);
	}
}
