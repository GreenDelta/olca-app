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
	public ConnectionAnchor getSourceConnectionAnchor(ConnectionEditPart con) {
		Link link = (Link) con.getModel();
		ProcessNode process = link.outputNode;
		ExchangeNode output = process.getOutput(link.processLink);
		return LinkAnchor.forOutput(output);
	}

	@Override
	public ConnectionAnchor getSourceConnectionAnchor(Request req) {
		if (req instanceof CreateConnectionRequest)
			return sourceAnchor((CreateConnectionRequest) req);
		if (req instanceof ReconnectRequest)
			return sourceAnchor((ReconnectRequest) req);
		return null;
	}

	private ConnectionAnchor sourceAnchor(CreateConnectionRequest req) {
		CreateLinkCommand cmd = (CreateLinkCommand) ((CreateConnectionRequest) req)
				.getStartCommand();
		if (cmd.output != null)
			return LinkAnchor.forOutput(cmd.output);
		if (cmd.input != null)
			return LinkAnchor.forInput(cmd.input);
		return null;
	}

	private ConnectionAnchor sourceAnchor(ReconnectRequest req) {
		Link link = (Link) req.getConnectionEditPart().getModel();
		ProcessNode node = ((ExchangePart) req.getTarget()).getModel().parent();
		ExchangeNode output = node.getOutput(link.processLink);
		ExchangeNode input = link.inputNode.getInput(link.processLink);
		if (input == null || !input.matches(output))
			return null;
		return LinkAnchor.forOutput(output);
	}

	@Override
	public ConnectionAnchor getTargetConnectionAnchor(ConnectionEditPart con) {
		Link link = (Link) con.getModel();
		ProcessNode process = link.inputNode;
		ExchangeNode input = process.getInput(link.processLink);
		return LinkAnchor.forInput(input);
	}

	@Override
	public ConnectionAnchor getTargetConnectionAnchor(Request req) {
		if (req instanceof CreateConnectionRequest)
			return targetAnchor((CreateConnectionRequest) req);
		if (req instanceof ReconnectRequest)
			return targetAnchor((ReconnectRequest) req);
		return null;
	}

	private ConnectionAnchor targetAnchor(CreateConnectionRequest req) {
		CreateLinkCommand cmd = (CreateLinkCommand) req.getStartCommand();
		if (cmd.startedFromOutput) {
			if (cmd.input != null)
				return LinkAnchor.forInput(cmd.input);
			return null;
		}
		if (cmd.output != null)
			return LinkAnchor.forOutput(cmd.output);
		return null;
	}

	private ConnectionAnchor targetAnchor(ReconnectRequest req) {
		Link link = (Link) req.getConnectionEditPart().getModel();
		ExchangeNode input = ((ExchangePart) req.getTarget()).getModel();
		ExchangeNode output = link.outputNode.getOutput(link.processLink);
		if (output == null || !output.matches(input))
			return null;

		// TODO: waste links
		if (input.exchange.getId() != link.processLink.exchangeId
				&& input.parent().isConnected(input.exchange.getId()))
			return null;

		return LinkAnchor.forInput(input);
	}
}
