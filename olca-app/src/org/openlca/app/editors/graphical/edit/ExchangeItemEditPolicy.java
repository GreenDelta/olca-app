package org.openlca.app.editors.graphical.edit;

import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.ConnectionRouter;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.GraphicalNodeEditPolicy;
import org.eclipse.gef.requests.CreateConnectionRequest;
import org.eclipse.gef.requests.ReconnectRequest;
import org.openlca.app.editors.graphical.figures.ExchangeFigure;
import org.openlca.app.editors.graphical.model.ExchangeItem;
import org.openlca.app.editors.graphical.model.GraphLink;
import org.openlca.app.editors.graphical.model.commands.CreateLinkCommand;
import org.openlca.app.editors.graphical.model.commands.ReconnectLinkCommand;

import java.util.Collection;
import java.util.Collections;

public class ExchangeItemEditPolicy extends GraphicalNodeEditPolicy {

	@Override
	protected Connection createDummyConnection(Request req) {
		var con = (PolylineConnection) super.createDummyConnection(req);
		var editor = ((ExchangeItem) getHost().getModel()).getGraph().getEditor();
		con.setForegroundColor(editor.config.getTheme().linkColor());
		if (!(req instanceof CreateConnectionRequest)) {
			con.setTargetDecoration(new PolygonDecoration());
			return con;
		}
		var cmd = (CreateLinkCommand) ((CreateConnectionRequest) req).getStartCommand();
		if (cmd.source != null)
			con.setTargetDecoration(new PolygonDecoration());
		else if (cmd.target != null)
			con.setSourceDecoration(new PolygonDecoration());
		return con;
	}

	@Override
	protected ConnectionRouter getDummyConnectionRouter(CreateConnectionRequest req) {
		return ConnectionRouter.NULL;
	}

	@Override
	protected Command getConnectionCompleteCommand(CreateConnectionRequest req) {
		var cmd = (CreateLinkCommand) req.getStartCommand();
		if (cmd == null)
			return null;
		var toConnect = (ExchangeItem) req.getTargetEditPart().getModel();
		var other = cmd.startedFromSource ? cmd.source : cmd.target;
		if (!toConnect.matches(other) || toConnect.isConnected()) {
			cmd.completeWith(null);
			req.setStartCommand(cmd);
			return null;
		}
		cmd.completeWith(toConnect);
		req.setStartCommand(cmd);
		if (cmd.source == null || cmd.target == null)
			return null;
		return cmd;
	}

	@Override
	protected Command getConnectionCreateCommand(CreateConnectionRequest req) {
		ExchangeItem toConnect = (ExchangeItem) req.getTargetEditPart().getModel();
		long flowId = toConnect.exchange.flow.id;
		if (!toConnect.exchange.isInput) {
			CreateLinkCommand cmd = new CreateLinkCommand(flowId);
			cmd.source = toConnect;
			cmd.startedFromSource = true;
			req.setStartCommand(cmd);
			return cmd;
		} else if (!toConnect.isConnected()) {
			CreateLinkCommand cmd = new CreateLinkCommand(flowId);
			cmd.target = toConnect;
			cmd.startedFromSource = false;
			req.setStartCommand(cmd);
			return cmd;
		}
		return null;
	}

	@Override
	protected Command getReconnectSourceCommand(ReconnectRequest request) {
		GraphLink link = (GraphLink) request.getConnectionEditPart().getModel();
		ExchangeItem toConnect = (ExchangeItem) request.getTarget().getModel();
		ExchangeItem other = link.getTargetNode().getInput(link.processLink);
		if (!toConnect.matches(other))
			return null;
		boolean sameNode = toConnect.matchesLink(link.processLink);
		if (!sameNode && toConnect.isConnected())
			return null;
		return new ReconnectLinkCommand(toConnect.getNode(), other, link);
	}

	@Override
	protected Command getReconnectTargetCommand(ReconnectRequest request) {
		GraphLink link = (GraphLink) request.getConnectionEditPart().getModel();
		ExchangeItem toConnect = (ExchangeItem) request.getTarget().getModel();
		ExchangeItem other = link.getSourceNode().getOutput(link.processLink);
		if (!toConnect.matches(other))
			return null;
		boolean sameNode = toConnect.matchesLink(link.processLink);
		if (!sameNode && toConnect.isConnected())
			return null;
		return new ReconnectLinkCommand(link.getSourceNode(), toConnect, link);
	}

	@Override
	public void eraseSourceFeedback(Request request) {
		highlightMatchingItems(false);
		super.eraseSourceFeedback(request);
	}

	@Override
	public void showSourceFeedback(Request request) {
		highlightMatchingItems(true);
		super.showSourceFeedback(request);
	}

	private void highlightMatchingItems(boolean b) {
		var exchangeItem = (ExchangeItem) getHost().getModel();
		var editor = exchangeItem.getGraph().getEditor();
		var viewer = (GraphicalViewer) editor.getAdapter(GraphicalViewer.class);
		var registry = viewer.getEditPartRegistry();

		@SuppressWarnings("unchecked")
		Collection<EditPart> parts = Collections.checkedCollection(registry.values(),
				EditPart.class);

		parts.stream()
				.filter(part -> part instanceof ExchangeEditPart)
				.map(ExchangeEditPart.class::cast)
				.filter(part -> !part.getModel().isConnected())
				.filter(part -> exchangeItem.matches(part.getModel()))
				.forEach(part -> ((ExchangeFigure) part.getFigure()).setHighlighted(b));
	}

}
