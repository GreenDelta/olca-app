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
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;

public class ProcessLinkCreatePolicy extends GraphicalNodeEditPolicy {

	private PolylineConnection connection;

	@Override
	protected Connection createDummyConnection(Request req) {
		connection = (PolylineConnection) super.createDummyConnection(req);
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
	protected Command getConnectionCompleteCommand(CreateConnectionRequest request) {
		if (request.getStartCommand() == null)
			return null;
		if (!(request.getTargetEditPart().getModel() instanceof ExchangeNode))
			return null;
		CreateLinkCommand cmd = (CreateLinkCommand) request.getStartCommand();
		ExchangeNode target = (ExchangeNode) request.getTargetEditPart().getModel();
		ProcessNode targetNode = target.parent();
		if (!target.exchange.isInput())
			cmd.sourceNode = targetNode;
		else if (!targetNode.hasIncomingConnection(cmd.flowId))
			cmd.targetNode = targetNode;
		request.setStartCommand(cmd);
		return cmd;
	}

	@Override
	protected Command getConnectionCreateCommand(CreateConnectionRequest request) {
		CreateLinkCommand cmd = null;
		ExchangeNode target = (ExchangeNode) request.getTargetEditPart().getModel();
		ProcessNode targetNode = target.parent();
		long flowId = target.exchange.getFlow().getId();
		if (!target.exchange.isInput()) {
			cmd = new CreateLinkCommand(flowId);
			cmd.sourceNode = targetNode;
			cmd.startedFromSource = true;
			request.setStartCommand(cmd);
		} else if (!targetNode.hasIncomingConnection(flowId)) {
			cmd = new CreateLinkCommand(flowId);
			cmd.targetNode = targetNode;
			cmd.startedFromSource = false;
			request.setStartCommand(cmd);
		}
		return cmd;
	}

	@Override
	protected ConnectionRouter getDummyConnectionRouter(CreateConnectionRequest request) {
		return ConnectionRouter.NULL;
	}

	@Override
	protected Command getReconnectSourceCommand(ReconnectRequest request) {
		if (!(request.getTarget().getModel() instanceof ExchangeNode))
			return null;
		Link link = (Link) request.getConnectionEditPart().getModel();
		ExchangeNode source = (ExchangeNode) request.getTarget().getModel();
		ProcessNode sourceNode = source.parent();
		return new ReconnectLinkCommand(sourceNode, link.targetNode, link);
	}

	@Override
	protected Command getReconnectTargetCommand(ReconnectRequest request) {
		if (!(request.getTarget().getModel() instanceof ExchangeNode))
			return null;
		Link link = (Link) request.getConnectionEditPart().getModel();
		ExchangeNode target = (ExchangeNode) request.getTarget().getModel();
		ProcessNode targetNode = target.parent();
		long flowId = link.processLink.flowId;
		boolean canConnect = true;
		if (!link.targetNode.equals(targetNode) && targetNode.hasIncomingConnection(flowId))
			canConnect = false;
		if (canConnect)
			return new ReconnectLinkCommand(link.sourceNode, targetNode, link);
		return null;
	}

	@Override
	public void eraseSourceFeedback(Request request) {
		if (!(getHost().getModel() instanceof ExchangeNode)) {
			super.eraseSourceFeedback(request);
			return;
		}
		ExchangeNode node = (ExchangeNode) getHost().getModel();
		ProductSystemNode psNode = node.parent().parent();
		psNode.removeHighlighting();
		node.setHighlighted(false);
	}

	@Override
	public void showSourceFeedback(Request request) {
		if (!(getHost().getModel() instanceof ExchangeNode)) {
			super.showSourceFeedback(request);
			return;
		}
		ExchangeNode node = (ExchangeNode) getHost().getModel();
		ProductSystemNode psNode = node.parent().parent();
		psNode.highlightMatchingExchanges(node);
		node.setHighlighted(true);
	}
}
