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
import org.eclipse.gef.editpolicies.ConnectionEditPolicy;
import org.eclipse.gef.editpolicies.ConnectionEndpointEditPolicy;
import org.eclipse.gef.requests.GroupRequest;
import org.eclipse.gef.requests.ReconnectRequest;
import org.openlca.app.editors.graphical.ProductSystemGraphEditor;
import org.openlca.app.editors.graphical.command.DeleteLinkCommand;

class LinkPart extends AbstractConnectionEditPart {

	@Override
	public void activate() {
		getModel().editPart = this;
		super.activate();
	}

	@Override
	protected IFigure createFigure() {
		PolylineConnection figure = new PolylineConnection();
		figure.setForegroundColor(Link.COLOR);
		figure.setConnectionRouter(getConnectionRouter());
		figure.setTargetDecoration(new PolygonDecoration());
		figure.setVisible(isVisible());
		getModel().figure = figure;
		return figure;
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.CONNECTION_ENDPOINTS_ROLE, new ConnectionEndpointEditPolicy());
		installEditPolicy(EditPolicy.CONNECTION_ROLE, new ConnectionEditPolicy() {
			@Override
			protected Command getDeleteCommand(GroupRequest arg0) {
				return new DeleteLinkCommand(getModel());
			}
		});
	}

	@Override
	public Link getModel() {
		return (Link) super.getModel();
	}

	private ProductSystemGraphEditor getEditor() {
		return getModel().sourceNode.parent().editor;
	}

	private ConnectionRouter getConnectionRouter() {
		return getEditor().isRouted() ? TreeConnectionRouter.instance : ConnectionRouter.NULL;
	}

	private boolean isVisible() {
		if (!getModel().sourceNode.isVisible())
			return false;
		if (!getModel().targetNode.isVisible())
			return false;
		return true;
	}

	@Override
	public void showSourceFeedback(Request req) {
		if (!(req instanceof ReconnectRequest)) {
			super.showSourceFeedback(req);
			return;
		}
		ReconnectRequest request = ((ReconnectRequest) req);
		Link link = (Link) request.getConnectionEditPart().getModel();
		ExchangeNode target = link.targetNode.getNode(link.processLink.exchangeId);
		ExchangeNode source = link.sourceNode.getOutput(link.processLink.flowId);
		ExchangeNode n1 = request.isMovingStartAnchor() ? target : source;
		ExchangeNode n2 = request.isMovingStartAnchor() ? source : target;
		if (n1 != null) {
			ProductSystemNode productSystemNode = n1.parent().parent();
			productSystemNode.highlightMatchingExchanges(n1);
			n1.setHighlighted(true);
		}
		if (n2 != null)
			n2.setHighlighted(true);
	}

	@Override
	public void eraseSourceFeedback(Request req) {
		if (!(req instanceof ReconnectRequest)) {
			super.eraseSourceFeedback(req);
			return;
		}
		ProcessPart source = (ProcessPart) getSource();
		ProductSystemNode node = source.getModel().parent();
		node.removeHighlighting();
	}

	@Override
	public boolean isSelectable() {
		return true;
	}

	@Override
	public void setSelected(int value) {
		if (!getFigure().isVisible())
			return;
		PolylineConnection figure = (PolylineConnection) getFigure();
		if (value != EditPart.SELECTED_NONE) {
			figure.setLineWidth(2);
			figure.setForegroundColor(Link.HIGHLIGHT_COLOR);
		} else {
			figure.setLineWidth(1);
			figure.setForegroundColor(Link.COLOR);
		}
		super.setSelected(value);
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
