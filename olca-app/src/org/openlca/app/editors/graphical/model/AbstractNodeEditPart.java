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
		return LinkAnchor.forOutput(link);
	}

	@Override
	public ConnectionAnchor getSourceConnectionAnchor(Request request) {
		if (request instanceof CreateConnectionRequest)
			return getSourceConnectionAnchor((CreateConnectionRequest) request);
		if (request instanceof ReconnectRequest)
			return getSourceConnectionAnchor((ReconnectRequest) request);
		return null;
	}

	private ConnectionAnchor getSourceConnectionAnchor(CreateConnectionRequest req) {
		CreateLinkCommand cmd = (CreateLinkCommand) ((CreateConnectionRequest) req)
				.getStartCommand();
		if (cmd.output != null)
			return LinkAnchor.forOutput(cmd.output.parent(), cmd.output);
		if (cmd.input != null)
			return LinkAnchor.forInput(cmd.input.parent(), cmd.input);
		return null;
	}

	private ConnectionAnchor getSourceConnectionAnchor(ReconnectRequest request) {
		Link link = (Link) request.getConnectionEditPart().getModel();
		ProcessNode node = ((ExchangePart) request.getTarget()).getModel().parent();
		ExchangeNode source = node.getOutput(link.processLink);
		ExchangeNode target = link.inputNode.getInput(link.processLink);
		if (target == null || !target.matches(source))
			return null;
		return LinkAnchor.forOutput(link);
	}

	@Override
	public ConnectionAnchor getTargetConnectionAnchor(ConnectionEditPart connection) {
		Link link = (Link) connection.getModel();
		return LinkAnchor.forInput(link);
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
		if (cmd.startedFromOutput) {
			if (cmd.input != null)
				return LinkAnchor.forInput(cmd.input.parent(), cmd.input);
			return null;
		}
		if (cmd.output != null)
			return LinkAnchor.forOutput(cmd.output.parent(), cmd.output);
		return null;
	}

	private ConnectionAnchor getTargetConnectionAnchor(ReconnectRequest request) {
		Link link = (Link) request.getConnectionEditPart().getModel();
		ExchangeNode target = ((ExchangePart) request.getTarget()).getModel();
		ExchangeNode source = link.outputNode.getOutput(link.processLink);
		if (source == null || !source.matches(target))
			return null;
		if (target.exchange.getId() != link.processLink.exchangeId
				&& target.parent().hasIncoming(target.exchange.getId()))
			return null;
		return LinkAnchor.forInput(target.parent(), target);
	}
}
