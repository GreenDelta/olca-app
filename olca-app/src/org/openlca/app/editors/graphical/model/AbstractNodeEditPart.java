package org.openlca.app.editors.graphical.model;

import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.requests.CreateConnectionRequest;
import org.eclipse.gef.requests.ReconnectRequest;
import org.openlca.app.editors.graphical.command.CreateLinkCommand;

abstract class AbstractNodeEditPart<N extends Node> extends
		AppAbstractEditPart<N>implements NodeEditPart {

	@Override
	public ConnectionAnchor getSourceConnectionAnchor(
			ConnectionEditPart connection) {
		ConnectionLink link = (ConnectionLink) connection.getModel();
		return LinkAnchor.createSourceAnchor(link.getSourceNode(), link);
	}

	@Override
	public ConnectionAnchor getTargetConnectionAnchor(
			ConnectionEditPart connection) {
		ConnectionLink link = (ConnectionLink) connection.getModel();
		return LinkAnchor.createTargetAnchor(link.getTargetNode(), link);
	}

	@Override
	public ConnectionAnchor getSourceConnectionAnchor(Request request) {
		if (request instanceof CreateConnectionRequest) {
			CreateLinkCommand cmd = (CreateLinkCommand) ((CreateConnectionRequest) request)
					.getStartCommand();
			if (cmd.getSourceNode() != null)
				return LinkAnchor.createSourceAnchor(cmd.getSourceNode(),
						cmd.getLink());
			else if (cmd.getTargetNode() != null)
				return LinkAnchor.createTargetAnchor(cmd.getTargetNode(),
						cmd.getLink());
		} else if (request instanceof ReconnectRequest) {
			ReconnectRequest req = (ReconnectRequest) request;
			ConnectionLink link = (ConnectionLink) req.getConnectionEditPart()
					.getModel();
			ProcessNode provider = ((ExchangePart) req.getTarget())
					.getModel().getParent().getParent();
			long flowId = link.getProcessLink().flowId;
			long exchangeId = link.getProcessLink().exchangeId;
			ExchangeNode source = provider.getOutputNode(flowId);
			ExchangeNode target = link.getTargetNode().getExchangeNode(exchangeId);
			if (target != null && target.matches(source))
				return LinkAnchor.createSourceAnchor(provider, link);
		}
		return null;
	}

	@Override
	public ConnectionAnchor getTargetConnectionAnchor(Request request) {
		if (request instanceof CreateConnectionRequest) {
			CreateLinkCommand cmd = (CreateLinkCommand) ((CreateConnectionRequest) request)
					.getStartCommand();
			if (cmd.isStartedFromSource()) {
				if (cmd.getTargetNode() != null)
					return LinkAnchor.createTargetAnchor(cmd.getTargetNode(),
							cmd.getLink());
				else if (cmd.getSourceNode() != null)
					return LinkAnchor.createSourceAnchor(cmd.getSourceNode(),
							cmd.getLink());
			} else {
				if (cmd.getSourceNode() != null)
					return LinkAnchor.createSourceAnchor(cmd.getSourceNode(),
							cmd.getLink());
				else if (cmd.getTargetNode() != null)
					return LinkAnchor.createTargetAnchor(cmd.getTargetNode(),
							cmd.getLink());
			}
		} else if (request instanceof ReconnectRequest) {
			ReconnectRequest req = (ReconnectRequest) request;
			ConnectionLink link = (ConnectionLink) req.getConnectionEditPart()
					.getModel();
			ProcessNode processNode = ((ExchangePart) req.getTarget())
					.getModel().getParent().getParent();
			long flowId = link.getProcessLink().flowId;
			long exchangeId = link.getProcessLink().exchangeId;
			ExchangeNode source = link.getSourceNode().getOutputNode(flowId);
			ExchangeNode target = processNode.getExchangeNode(exchangeId);
			if (source != null && source.matches(target)) {
				if (canConnect(link.getTargetNode().getExchangeNode(exchangeId),
						target))
					return LinkAnchor.createTargetAnchor(processNode, link);
			}
		}
		return null;
	}

	private boolean canConnect(ExchangeNode node, ExchangeNode withNode) {
		if (withNode
				.getParent()
				.getParent()
				.hasIncomingConnection(withNode.getExchange().getFlow().getId()))
			return false;
		if (node.getExchange().getFlow()
				.equals(withNode.getExchange().getFlow()))
			return false;
		if (node == withNode)
			return false;
		return true;
	}

}
