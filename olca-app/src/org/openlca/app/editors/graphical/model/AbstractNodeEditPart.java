package org.openlca.app.editors.graphical.model;

import java.util.Objects;

import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.requests.CreateConnectionRequest;
import org.eclipse.gef.requests.ReconnectRequest;
import org.openlca.app.editors.graphical.GraphUtil;
import org.openlca.app.editors.graphical.command.CreateLinkCommand;

abstract class AbstractNodeEditPart<N extends Node> extends
		AppAbstractEditPart<N>implements NodeEditPart {

	@Override
	public ConnectionAnchor getSourceConnectionAnchor(
			ConnectionEditPart connection) {
		ConnectionLink link = (ConnectionLink) connection.getModel();
		return LinkAnchor.createSourceAnchor(link.sourceNode, link);
	}

	@Override
	public ConnectionAnchor getTargetConnectionAnchor(
			ConnectionEditPart connection) {
		ConnectionLink link = (ConnectionLink) connection.getModel();
		return LinkAnchor.createTargetAnchor(link.targetNode, link);
	}

	@Override
	public ConnectionAnchor getSourceConnectionAnchor(Request request) {
		if (request instanceof CreateConnectionRequest) {
			CreateLinkCommand cmd = (CreateLinkCommand) ((CreateConnectionRequest) request)
					.getStartCommand();
			if (cmd.providerNode != null)
				return LinkAnchor.createSourceAnchor(cmd.providerNode,
						cmd.getLink());
			else if (cmd.exchangeNode != null)
				return LinkAnchor.createTargetAnchor(cmd.exchangeNode,
						cmd.getLink());
		} else if (request instanceof ReconnectRequest) {
			ReconnectRequest req = (ReconnectRequest) request;
			ConnectionLink link = (ConnectionLink) req.getConnectionEditPart()
					.getModel();
			ProcessNode provider = ((ExchangePart) req.getTarget())
					.getModel().getParent().getParent();
			long flowId = link.processLink.flowId;
			long exchangeId = link.processLink.exchangeId;
			ExchangeNode source = provider.getProviderNode(flowId);
			ExchangeNode target = link.targetNode.getExchangeNode(exchangeId);
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
			if (cmd.providerNode != null)
				return LinkAnchor.createSourceAnchor(cmd.providerNode, cmd.getLink());
			if (cmd.exchangeNode != null)
				return LinkAnchor.createTargetAnchor(cmd.exchangeNode, cmd.getLink());
		} else if (request instanceof ReconnectRequest) {
			ReconnectRequest req = (ReconnectRequest) request;
			ConnectionLink link = (ConnectionLink) req.getConnectionEditPart()
					.getModel();
			ProcessNode processNode = ((ExchangePart) req.getTarget())
					.getModel().getParent().getParent();
			long flowId = link.processLink.flowId;
			long exchangeId = link.processLink.exchangeId;
			ExchangeNode source = link.sourceNode.getProviderNode(flowId);
			ExchangeNode target = processNode.getExchangeNode(exchangeId);
			if (source != null && source.matches(target)) {
				if (canConnect(link.targetNode.getExchangeNode(exchangeId),
						target))
					return LinkAnchor.createTargetAnchor(processNode, link);
			}
		}
		return null;
	}

	private boolean canConnect(ExchangeNode node, ExchangeNode with) {
		if (node == with)
			return false;
		ProcessNode withProcess = GraphUtil.getProcessNode(with);
		if (withProcess.isLinkedExchange(with.getExchange().getId()))
			return false;
		return Objects.equals(node.getExchange().getFlow(),
				with.getExchange().getFlow());
	}
}
