package org.openlca.app.editors.graphical_legacy.model;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.ConnectionRouter;
import org.eclipse.draw2d.Graphics;
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
import org.openlca.app.editors.graphical_legacy.GraphEditor;
import org.openlca.app.editors.graphical_legacy.command.DeleteLinkCommand;
import org.openlca.app.editors.graphical_legacy.view.TreeConnectionRouter;

class LinkPart extends AbstractConnectionEditPart {

	@Override
	public void activate() {
		getModel().editPart = this;
		super.activate();
	}

	@Override
	protected IFigure createFigure() {
		var figure = new PolylineConnection() {
			@Override
			public void paint(Graphics g) {
				var link = getModel();
				var provider = link.provider();
				var theme = provider != null
						? provider.config().theme()
						: null;
				if (theme != null) {
					setForegroundColor(theme.linkColor());
				} else {
					setForegroundColor(ColorConstants.black);
				}
				super.paint(g);
			}
		};
		figure.setConnectionRouter(getConnectionRouter());
		figure.setTargetDecoration(new PolygonDecoration());
		figure.setVisible(isVisible());
		figure.setLineWidth(1);
		getModel().figure = figure;
		return figure;
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(
			EditPolicy.CONNECTION_ENDPOINTS_ROLE,
			new ConnectionEndpointEditPolicy());
		installEditPolicy(
			EditPolicy.CONNECTION_ROLE,
			new ConnectionEditPolicy() {
			@Override
			protected Command getDeleteCommand(GroupRequest _req) {
				return new DeleteLinkCommand(getModel());
			}
		});
	}

	@Override
	public Link getModel() {
		return (Link) super.getModel();
	}

	private GraphEditor getEditor() {
		return getModel().outputNode.parent().editor;
	}

	private ConnectionRouter getConnectionRouter() {
		return getEditor().config.isRouted
				? TreeConnectionRouter.instance
				: ConnectionRouter.NULL;
	}

	private boolean isVisible() {
		if (!getModel().outputNode.isVisible())
			return false;
		return getModel().inputNode.isVisible();
	}

	@Override
	public void showSourceFeedback(Request req) {
		var request = ((ReconnectRequest) req);
		Link link = (Link) request.getConnectionEditPart().getModel();
		ExchangeNode target = link.inputNode.getInput(link.processLink);
		ExchangeNode source = link.outputNode.getOutput(link.processLink);
		ExchangeNode n1 = request.isMovingStartAnchor() ? target : source;
		ExchangeNode n2 = request.isMovingStartAnchor() ? source : target;
		if (n1 != null) {
			ProductSystemNode productSystemNode = n1.parent().parent();
			productSystemNode.highlightMatchingExchanges(n1);
			n1.setHighlighted(true);
		}
		if (n2 != null)
			n2.setHighlighted(true);
		super.showSourceFeedback(req);
	}

	@Override
	public void eraseSourceFeedback(Request req) {
		var source = (ProcessPart) getSource();
		ProductSystemNode node = source.getModel().parent();
		node.removeHighlighting();
		super.eraseSourceFeedback(req);
	}

	@Override
	public boolean isSelectable() {
		return true;
	}

	@Override
	public void setSelected(int value) {
		if (!getFigure().isVisible())
			return;
		var figure = (PolylineConnection) getFigure();
		if (value != EditPart.SELECTED_NONE) {
			figure.setLineWidth(2);
		} else {
			figure.setLineWidth(1);
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
