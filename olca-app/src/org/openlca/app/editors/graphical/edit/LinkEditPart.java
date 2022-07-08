package org.openlca.app.editors.graphical.edit;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.draw2d.*;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.gef.editpolicies.ConnectionEditPolicy;
import org.eclipse.gef.editpolicies.ConnectionEndpointEditPolicy;
import org.eclipse.gef.requests.GroupRequest;
import org.openlca.app.editors.graphical.model.Link;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.model.commands.DeleteLinkCommand;

public class LinkEditPart extends AbstractConnectionEditPart
	implements PropertyChangeListener {

	public void activate() {
		super.activate();
		getModel().addPropertyChangeListener(this);
	}

	public void activateFigure() {
		super.activateFigure();
		/*
		 * Once the figure has been added to the ConnectionLayer, start
		 * listening for its router to change.
		 */
		getFigure().addPropertyChangeListener(
			Connection.PROPERTY_CONNECTION_ROUTER, this);
	}

	public void deactivate() {
		getModel().removePropertyChangeListener(this);
		super.deactivate();
	}

	public void deactivateFigure() {
		getFigure().removePropertyChangeListener(
			Connection.PROPERTY_CONNECTION_ROUTER, this);
		super.deactivateFigure();
	}

	@Override
	protected IFigure createFigure() {
		var figure = new PolylineConnection() {
			@Override
			public void paint(Graphics g) {
				var link = getModel();
				var provider = link.provider();
				var theme = provider != null
					? provider.editor.config.getTheme()
					: null;
				if (theme != null) {
					setForegroundColor(theme.linkColor());
				} else {
					setForegroundColor(ColorConstants.black);
				}
				super.paint(g);
			}
		};
		figure.setTargetDecoration(new PolygonDecoration());
		figure.setLineWidth(1);
		return figure;
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.CONNECTION_ENDPOINTS_ROLE,
			new ConnectionEndpointEditPolicy());
		installEditPolicy(
			EditPolicy.CONNECTION_ROLE, new ConnectionEditPolicy() {
				@Override
				protected Command getDeleteCommand(GroupRequest req) {
					return new DeleteLinkCommand(getModel());
				}
			});
	}

	public Link getModel() {
		return (Link) super.getModel();
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {}

}
