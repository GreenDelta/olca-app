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
		return new ConnectionLinkAnchor(link.getSourceNode(), link,
				ConnectionLinkAnchor.SOURCE_ANCHOR);
	}

	@Override
	public ConnectionAnchor getTargetConnectionAnchor(
			ConnectionEditPart connection) {
		ConnectionLink link = (ConnectionLink) connection.getModel();
		return new ConnectionLinkAnchor(link.getTargetNode(), link,
				ConnectionLinkAnchor.TARGET_ANCHOR);
	}

	@Override
	public ConnectionAnchor getSourceConnectionAnchor(Request request) {
		if (request instanceof CreateConnectionRequest) {
			CreateLinkCommand cmd = (CreateLinkCommand) ((CreateConnectionRequest) request)
					.getStartCommand();
			if (cmd.getSourceNode() != null)
				return new ConnectionLinkAnchor(cmd.getSourceNode(),
						cmd.getLink(), ConnectionLinkAnchor.SOURCE_ANCHOR);
			else if (cmd.getTargetNode() != null)
				return new ConnectionLinkAnchor(cmd.getTargetNode(),
						cmd.getLink(), ConnectionLinkAnchor.TARGET_ANCHOR);
		} else if (request instanceof ReconnectRequest) {
			ReconnectRequest req = (ReconnectRequest) request;
			ConnectionLink link = (ConnectionLink) req.getConnectionEditPart()
					.getModel();
			ProcessNode processNode = ((ExchangePart) req.getTarget())
					.getModel().getParent().getParent();
			long flowId = link.getProcessLink().flowId;
			ExchangeNode source = processNode.getOutputNode(flowId);
			ExchangeNode target = link.getTargetNode().getInputNode(flowId);
			if (target != null && target.matches(source))
				return new ConnectionLinkAnchor(processNode, link,
						ConnectionLinkAnchor.SOURCE_ANCHOR);
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
					return new ConnectionLinkAnchor(cmd.getTargetNode(),
							cmd.getLink(), ConnectionLinkAnchor.TARGET_ANCHOR);
				else if (cmd.getSourceNode() != null)
					return new ConnectionLinkAnchor(cmd.getSourceNode(),
							cmd.getLink(), ConnectionLinkAnchor.SOURCE_ANCHOR);
			} else {
				if (cmd.getSourceNode() != null)
					return new ConnectionLinkAnchor(cmd.getSourceNode(),
							cmd.getLink(), ConnectionLinkAnchor.SOURCE_ANCHOR);
				else if (cmd.getTargetNode() != null)
					return new ConnectionLinkAnchor(cmd.getTargetNode(),
							cmd.getLink(), ConnectionLinkAnchor.TARGET_ANCHOR);
			}
		} else if (request instanceof ReconnectRequest) {
			ReconnectRequest req = (ReconnectRequest) request;
			ConnectionLink link = (ConnectionLink) req.getConnectionEditPart()
					.getModel();
			ProcessNode processNode = ((ExchangePart) req.getTarget())
					.getModel().getParent().getParent();
			long flowId = link.getProcessLink().flowId;
			ExchangeNode source = link.getSourceNode().getOutputNode(flowId);
			ExchangeNode target = processNode.getInputNode(flowId);
			if (source != null && source.matches(target))
				if (canConnect(link.getTargetNode().getInputNode(flowId),
						target))
					return new ConnectionLinkAnchor(processNode, link,
							ConnectionLinkAnchor.TARGET_ANCHOR);
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
