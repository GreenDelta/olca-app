package org.openlca.app.editors.graphical.edit;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;

import org.eclipse.draw2d.*;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.gef.editpolicies.ConnectionEditPolicy;
import org.eclipse.gef.editpolicies.ConnectionEndpointEditPolicy;
import org.eclipse.gef.requests.GroupRequest;
import org.openlca.app.tools.graphics.figures.CurvedConnection;
import org.openlca.app.editors.graphical.model.GraphLink;
import org.openlca.app.editors.graphical.model.commands.DeleteLinkCommand;

import static org.eclipse.swt.SWT.ON;
import static org.openlca.app.editors.graphical.GraphConfig.*;
import static org.openlca.app.editors.graphical.model.Graph.ORIENTATION;

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
		var config = getModel().getSourceNode().getGraph().getConfig();

		var provider = getModel().provider();
		var theme = provider != null
				? provider.getGraph().getEditor().config.getTheme()
				: null;
		var color = theme != null ? theme.linkColor() : ColorConstants.black;

		var connection = Objects.equals(config.connectionRouter(), ROUTER_CURVE)
				? new CurvedConnection(ORIENTATION) {
			@Override
			public void paint(Graphics g) {
				setAntialias(ON);
				setForegroundColor(color);
				super.paint(g);
			}
		}
				: new PolylineConnection() {
			@Override
			public void paint(Graphics g) {
				setAntialias(ON);
				setForegroundColor(color);
				super.paint(g);
			}
		};
		connection.setTargetDecoration(new PolygonDecoration());
		connection.setLineWidth(1);
		return connection;
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

	@Override
	public GraphLink getModel() {
		return (GraphLink) super.getModel();
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {}

}
