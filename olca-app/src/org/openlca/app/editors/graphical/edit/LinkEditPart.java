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
import org.openlca.app.tools.graphics.figures.Connection;
import org.openlca.app.editors.graphical.model.GraphLink;
import org.openlca.app.editors.graphical.model.commands.DeleteLinkCommand;

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
			org.eclipse.draw2d.Connection.PROPERTY_CONNECTION_ROUTER, this);
	}

	public void deactivate() {
		getModel().removePropertyChangeListener(this);
		super.deactivate();
	}

	public void deactivateFigure() {
		getFigure().removePropertyChangeListener(
			org.eclipse.draw2d.Connection.PROPERTY_CONNECTION_ROUTER, this);
		super.deactivateFigure();
	}

	@Override
	protected IFigure createFigure() {
		var graph = getModel().getSourceNode().getGraph();
		var config = graph.getConfig();
		var theme = config.getTheme();

		var router = config.connectionRouter();
		var color = theme != null ? theme.linkColor() : ColorConstants.black;
		var colorSelected = theme != null
				? theme.linkColorSelected()
				: ColorConstants.black;

		var connection = new Connection(router, ORIENTATION, color, colorSelected,
				graph) {
			@Override
			public void paint(Graphics g) {
				setLineWidth(isSelected() ? 2 : 1);
				super.paint(g);
			}
		};
		connection.setTargetDecoration(new PolygonDecoration());
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
