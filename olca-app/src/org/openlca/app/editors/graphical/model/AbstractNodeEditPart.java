package org.openlca.app.editors.graphical.model;

import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.requests.CreateConnectionRequest;
import org.eclipse.gef.requests.ReconnectRequest;
import org.openlca.app.editors.graphical.command.CreateLinkCommand;

abstract class AbstractNodeEditPart<N extends Node> extends AppAbstractEditPart<N> implements NodeEditPart {

	@Override
	public ConnectionAnchor getSourceConnectionAnchor(ConnectionEditPart connection) {
		Link link = (Link) connection.getModel();
		return LinkAnchor.newSourceAnchor(link);
	}

	@Override
	public ConnectionAnchor getSourceConnectionAnchor(Request request) {
		if (request instanceof CreateConnectionRequest)
			return getSourceConnectionAnchor((CreateConnectionRequest) request);
		if (request instanceof ReconnectRequest)
			return getSourceConnectionAnchor((ReconnectRequest) request);
		return null;
	}

	private ConnectionAnchor getSourceConnectionAnchor(CreateConnectionRequest request) {
		CreateLinkCommand cmd = (CreateLinkCommand) ((CreateConnectionRequest) request).getStartCommand();
		if (cmd.sourceNode != null)
			return LinkAnchor.newSourceAnchor(cmd);
		if (cmd.targetNode != null)
			return LinkAnchor.newTargetAnchor(cmd);
		return null;
	}

	private ConnectionAnchor getSourceConnectionAnchor(ReconnectRequest request) {
		Link link = (Link) request.getConnectionEditPart().getModel();
		ProcessNode node = ((ExchangePart) request.getTarget()).getModel().parent();
		long flowId = link.processLink.flowId;
		long exchangeId = link.processLink.exchangeId;
		ExchangeNode source = node.getOutput(flowId);
		ExchangeNode target = link.targetNode.getNode(exchangeId);
		if (target == null || !target.matches(source))
			return null;
		return LinkAnchor.newSourceAnchor(node, source);
	}

	@Override
	public ConnectionAnchor getTargetConnectionAnchor(ConnectionEditPart connection) {
		Link link = (Link) connection.getModel();
		return LinkAnchor.newTargetAnchor(link);
	}

	@Override
	public ConnectionAnchor getTargetConnectionAnchor(Request request) {
		if (request instanceof CreateConnectionRequest)
			return getTargetConnectionAnchor((CreateConnectionRequest) request);
		if (request instanceof ReconnectRequest)
			return getTargetConnectionAnchor((ReconnectRequest) request);
		return null;
	}

	private ConnectionAnchor getTargetConnectionAnchor(CreateConnectionRequest request) {
		CreateLinkCommand cmd = (CreateLinkCommand) request.getStartCommand();
		if (cmd.startedFromSource) {
			if (cmd.targetNode != null)
				return LinkAnchor.newTargetAnchor(cmd);
			return null;
		}
		if (cmd.sourceNode != null)
			return LinkAnchor.newSourceAnchor(cmd);
		return null;
	}

	private ConnectionAnchor getTargetConnectionAnchor(ReconnectRequest request) {
		Link link = (Link) request.getConnectionEditPart().getModel();
		ExchangeNode target = ((ExchangePart) request.getTarget()).getModel();
		ExchangeNode source = link.sourceNode.getOutput(link.processLink.flowId);
		if (source == null || !source.matches(target))
			return null;
		if (target.exchange.getId() != link.processLink.exchangeId
				&& target.parent().hasIncoming(target.exchange.getId()))
			return null;
		return LinkAnchor.newTargetAnchor(target.parent(), target);
	}
}
