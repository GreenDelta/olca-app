package org.openlca.app.editors.graphical.model;

import org.eclipse.draw2d.ConnectionRouter;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.gef.editpolicies.ComponentEditPolicy;
import org.eclipse.gef.editpolicies.ConnectionEndpointEditPolicy;
import org.eclipse.gef.requests.GroupRequest;
import org.eclipse.gef.requests.ReconnectRequest;
import org.openlca.app.editors.graphical.ProductSystemGraphEditor;
import org.openlca.app.editors.graphical.command.CommandFactory;

class ConnectionLinkPart extends AbstractConnectionEditPart {

	@Override
	public void activate() {
		getModel().setEditPart(this);
		super.activate();
	}

	@Override
	protected IFigure createFigure() {
		PolylineConnection figure = new PolylineConnection();
		figure.setForegroundColor(ConnectionLink.COLOR);
		figure.setConnectionRouter(getConnectionRouter());
		figure.setTargetDecoration(new PolygonDecoration());
		figure.setVisible(isVisible());
		getModel().setFigure(figure);
		return figure;
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.CONNECTION_ENDPOINTS_ROLE,
				new ConnectionEndpointEditPolicy());
		installEditPolicy(EditPolicy.COMPONENT_ROLE, new ComponentEditPolicy() {
		});
	}

	@Override
	public ConnectionLink getModel() {
		return (ConnectionLink) super.getModel();
	}

	private ProductSystemGraphEditor getEditor() {
		return getModel().getSourceNode().getParent().getEditor();
	}

	private ConnectionRouter getConnectionRouter() {
		return getEditor().isRouted() ? TreeConnectionRouter.get()
				: ConnectionRouter.NULL;
	}

	private boolean isVisible() {
		if (!getModel().getSourceNode().getFigure().isVisible())
			return false;
		if (!getModel().getTargetNode().getFigure().isVisible())
			return false;
		return true;
	}

	@Override
	public void showSourceFeedback(Request req) {
		if (req instanceof ReconnectRequest) {
			ReconnectRequest request = ((ReconnectRequest) req);
			ConnectionLink link = (ConnectionLink) request
					.getConnectionEditPart().getModel();
			ExchangeNode target = link.getTargetNode().getInputNode(
					link.getProcessLink().getFlowId());
			ExchangeNode source = link.getSourceNode().getOutputNode(
					link.getProcessLink().getFlowId());

			ExchangeNode n1 = request.isMovingStartAnchor() ? target : source;
			ExchangeNode n2 = request.isMovingStartAnchor() ? source : target;
			if (n1 != null) {
				ProductSystemNode productSystemNode = n1.getParent()
						.getParent().getParent();
				productSystemNode.highlightMatchingExchanges(n1);
				n1.setHighlighted(true);
			}
			if (n2 != null)
				n2.setHighlighted(true);
		}
		super.showSourceFeedback(req);
	}

	@Override
	public void eraseSourceFeedback(Request req) {
		if (req instanceof ReconnectRequest) {
			ProcessPart source = (ProcessPart) getSource();
			ProductSystemNode productSystemNode = source.getModel().getParent();
			productSystemNode.removeHighlighting();
		}
		super.eraseSourceFeedback(req);
	}

	@Override
	public Command getCommand(Request request) {
		if (request instanceof GroupRequest && request.getType() == REQ_DELETE)
			return CommandFactory.createDeleteLinkCommand(getModel());
		return super.getCommand(request);
	}

	@Override
	public boolean isSelectable() {
		return true;
	}

	@Override
	public void setSelected(int value) {
		if (getFigure().isVisible()) {
			PolylineConnection figure = (PolylineConnection) getFigure();
			if (value != EditPart.SELECTED_NONE) {
				figure.setLineWidth(2);
				figure.setForegroundColor(ConnectionLink.HIGHLIGHT_COLOR);
			} else {
				figure.setLineWidth(1);
				figure.setForegroundColor(ConnectionLink.COLOR);
			}
			super.setSelected(value);
		}
	}

	@Override
	public void refreshSourceAnchor() {
		// make public
		super.refreshSourceAnchor();
	}

	@Override
	public void refreshTargetAnchor() {
		// make public
		super.refreshTargetAnchor();
	}

}
