package org.openlca.app.editors.graphical.model;

import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.requests.CreateConnectionRequest;
import org.eclipse.gef.requests.ReconnectRequest;
import org.openlca.app.editors.graphical.command.CreateLinkCommand;
import org.openlca.core.model.Flow;

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
		ProcessNode processNode = ((ExchangePart) request.getTarget()).getModel().parent();
		long flowId = link.processLink.flowId;
		long exchangeId = link.processLink.exchangeId;
		ExchangeNode source = processNode.getOutput(flowId);
		ExchangeNode target = link.targetNode.getNode(exchangeId);
		if (target == null || !target.matches(source))
			return null;
		return LinkAnchor.newSourceAnchor(processNode, link.processLink);
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
			if (cmd.sourceNode != null)
				return LinkAnchor.newSourceAnchor(cmd);
			return null;
		}
		if (cmd.sourceNode != null)
			return LinkAnchor.newSourceAnchor(cmd);
		if (cmd.targetNode != null)
			return LinkAnchor.newTargetAnchor(cmd);
		return null;
	}

	private ConnectionAnchor getTargetConnectionAnchor(ReconnectRequest request) {
		Link link = (Link) request.getConnectionEditPart().getModel();
		ExchangeNode target = ((ExchangePart) request.getTarget()).getModel();
		long flowId = link.processLink.flowId;
		long exchangeId = link.processLink.exchangeId;
		ExchangeNode source = link.sourceNode.getOutput(flowId);
		if (source == null || !source.matches(target))
			return null;
		if (!canConnect(link.targetNode.getNode(exchangeId), target))
			return null;
		return LinkAnchor.newTargetAnchor(target.parent(), link.processLink);
	}

	private boolean canConnect(ExchangeNode node, ExchangeNode withNode) {
		ProcessNode parentNode = withNode.parent();
		Flow flow = node.exchange.getFlow();
		Flow withFlow = withNode.exchange.getFlow();
		if (parentNode.hasIncoming(node.exchange.getId()))
			return false;
		if (flow.equals(withFlow))
			return false;
		if (node == withNode)
			return false;
		return true;
	}

}
