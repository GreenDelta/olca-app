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
		return LinkAnchor.createOutputAnchor(link.provider, link);
	}

	@Override
	public ConnectionAnchor getTargetConnectionAnchor(
			ConnectionEditPart connection) {
		ConnectionLink link = (ConnectionLink) connection.getModel();
		return LinkAnchor.createInputAnchor(link.exchange, link);
	}

	@Override
	public ConnectionAnchor getSourceConnectionAnchor(Request request) {
		if (request instanceof CreateConnectionRequest) {
			CreateLinkCommand cmd = (CreateLinkCommand) ((CreateConnectionRequest) request)
					.getStartCommand();
			ConnectionLink link = cmd.getLink();
			if (cmd.providerNode != null && cmd.startedFromProvider)
				return LinkAnchor.createOutputAnchor(cmd.providerNode, link);
			else if (cmd.exchangeNode != null)
				return LinkAnchor.createInputAnchor(cmd.exchangeNode, link);
		} else if (request instanceof ReconnectRequest) {
			ReconnectRequest req = (ReconnectRequest) request;
			ConnectionLink link = (ConnectionLink) req.getConnectionEditPart()
					.getModel();
			ExchangeNode onNode = (ExchangeNode) req.getTarget().getModel();
			System.out.println("sourceAnchor; onNode=" + onNode);

			ProcessNode provider = ((ExchangePart) req.getTarget())
					.getModel().getParent().getParent();
			long flowId = link.processLink.flowId;

			ExchangeNode source = provider.getProviderNode(flowId);
			ExchangeNode target = link.exchange;
			if (target != null && target.matches(source))
				return LinkAnchor.createOutputAnchor(provider, link);
		}
		return null;
	}

	@Override
	public ConnectionAnchor getTargetConnectionAnchor(Request request) {
		if (request instanceof CreateConnectionRequest) {
			CreateLinkCommand cmd = (CreateLinkCommand) ((CreateConnectionRequest) request)
					.getStartCommand();
			ConnectionLink link = cmd.getLink();
			if (cmd.exchangeNode != null && cmd.startedFromProvider)
				return LinkAnchor.createInputAnchor(cmd.exchangeNode, link);
			else if (cmd.providerNode != null)
				return LinkAnchor.createOutputAnchor(cmd.providerNode, link);
		} else if (request instanceof ReconnectRequest) {
			ReconnectRequest req = (ReconnectRequest) request;
			ConnectionLink link = (ConnectionLink) req.getConnectionEditPart()
					.getModel();
			ExchangeNode exchange = (ExchangeNode) req.getTarget().getModel();
			ExchangeNode provider = link.provider;
			if (provider != null && provider.matches(exchange)) {
				if (canConnect(link.exchange, exchange))
					return LinkAnchor.createInputAnchor(exchange, link);
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
