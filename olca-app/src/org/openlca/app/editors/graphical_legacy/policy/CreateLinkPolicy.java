package org.openlca.app.editors.graphical_legacy.policy;

import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.ConnectionRouter;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.GraphicalNodeEditPolicy;
import org.eclipse.gef.requests.CreateConnectionRequest;
import org.eclipse.gef.requests.ReconnectRequest;
import org.openlca.app.editors.graphical_legacy.GraphEditor;
import org.openlca.app.editors.graphical_legacy.command.CreateLinkCommand;
import org.openlca.app.editors.graphical_legacy.command.ReconnectLinkCommand;
import org.openlca.app.editors.graphical_legacy.model.ExchangeNode;
import org.openlca.app.editors.graphical_legacy.model.Link;
import org.openlca.app.editors.graphical_legacy.model.ProductSystemNode;

public class CreateLinkPolicy extends GraphicalNodeEditPolicy {

	private final GraphEditor editor;

	public CreateLinkPolicy(GraphEditor editor) {
		this.editor = editor;
	}

	@Override
	protected Connection createDummyConnection(Request req) {
		var con = (PolylineConnection) super.createDummyConnection(req);
		con.setForegroundColor(editor.config.theme().linkColor());
		if (!(req instanceof CreateConnectionRequest)) {
			con.setTargetDecoration(new PolygonDecoration());
			return con;
		}
		var cmd = (CreateLinkCommand) ((CreateConnectionRequest) req).getStartCommand();
		if (cmd.output != null)
			con.setTargetDecoration(new PolygonDecoration());
		else if (cmd.input != null)
			con.setSourceDecoration(new PolygonDecoration());
		return con;
	}

	@Override
	protected ConnectionRouter getDummyConnectionRouter(CreateConnectionRequest req) {
		return ConnectionRouter.NULL;
	}

	@Override
	protected Command getConnectionCompleteCommand(CreateConnectionRequest req) {
		CreateLinkCommand cmd = (CreateLinkCommand) req.getStartCommand();
		if (cmd == null)
			return null;
		ExchangeNode toConnect = (ExchangeNode) req.getTargetEditPart().getModel();
		ExchangeNode other = cmd.startedFromOutput ? cmd.output : cmd.input;
		if (!toConnect.matches(other) || toConnect.isConnected()) {
			cmd.completeWith(null);
			req.setStartCommand(cmd);
			return null;
		}
		cmd.completeWith(toConnect);
		req.setStartCommand(cmd);
		if (cmd.output == null || cmd.input == null)
			return null;
		return cmd;
	}

	@Override
	protected Command getConnectionCreateCommand(CreateConnectionRequest req) {
		ExchangeNode toConnect = (ExchangeNode) req.getTargetEditPart().getModel();
		long flowId = toConnect.exchange.flow.id;
		if (!toConnect.exchange.isInput) {
			CreateLinkCommand cmd = new CreateLinkCommand(flowId);
			cmd.output = toConnect;
			cmd.startedFromOutput = true;
			req.setStartCommand(cmd);
			return cmd;
		} else if (!toConnect.isConnected()) {
			CreateLinkCommand cmd = new CreateLinkCommand(flowId);
			cmd.input = toConnect;
			cmd.startedFromOutput = false;
			req.setStartCommand(cmd);
			return cmd;
		}
		return null;
	}

	@Override
	protected Command getReconnectSourceCommand(ReconnectRequest request) {
		Link link = (Link) request.getConnectionEditPart().getModel();
		ExchangeNode toConnect = (ExchangeNode) request.getTarget().getModel();
		ExchangeNode other = link.inputNode.getInput(link.processLink);
		if (!toConnect.matches(other))
			return null;
		return new ReconnectLinkCommand(toConnect.parent(), other, link);
	}

	@Override
	protected Command getReconnectTargetCommand(ReconnectRequest request) {
		Link link = (Link) request.getConnectionEditPart().getModel();
		ExchangeNode toConnect = (ExchangeNode) request.getTarget().getModel();
		ExchangeNode other = link.outputNode.getOutput(link.processLink);
		if (!toConnect.matches(other))
			return null;
		boolean sameNode = toConnect.exchange.id == link.processLink.exchangeId;
		if (!sameNode && toConnect.isConnected())
			return null;
		return new ReconnectLinkCommand(link.outputNode, toConnect, link);
	}

	@Override
	public void eraseSourceFeedback(Request request) {
		ExchangeNode node = (ExchangeNode) getHost().getModel();
		ProductSystemNode psNode = node.parent().parent();
		psNode.removeHighlighting();
		node.setHighlighted(false);
		super.eraseSourceFeedback(request);
	}

	@Override
	public void showSourceFeedback(Request request) {
		ExchangeNode node = (ExchangeNode) getHost().getModel();
		ProductSystemNode psNode = node.parent().parent();
		psNode.highlightMatchingExchanges(node);
		node.setHighlighted(true);
		super.showSourceFeedback(request);
	}
}
