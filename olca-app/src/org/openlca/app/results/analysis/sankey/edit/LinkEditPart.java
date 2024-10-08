package org.openlca.app.results.analysis.sankey.edit;

import static org.openlca.app.components.graphics.figures.Connection.ROUTER_CURVE;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.gef.editpolicies.ConnectionEndpointEditPolicy;
import org.openlca.app.results.analysis.sankey.model.SankeyLink;
import org.openlca.app.components.graphics.figures.Connection;

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
		var diagram = getModel().getSourceNode().getDiagram();
		var config = diagram.getConfig();
		var theme = diagram.editor.getTheme();

		var color = theme == null
				? ColorConstants.black
				: theme.linkColor(getModel().getSourceNode().linkType());
		var colorSelected = theme != null
				? theme.linkColorSelected()
				: ColorConstants.black;

		var orientation = getModel().getSourceNode().getDiagram().orientation;
		var router = config.connectionRouter();
		var connection = new Connection(router, orientation, color, colorSelected,
				diagram) {
				@Override
				public void paint(Graphics g) {
					setAlpha(180);
					super.paint(g);
				}
			};

		if (Objects.equals(config.connectionRouter(), ROUTER_CURVE))
			connection.setOffset(100);
		connection.setLineWidth(getModel().getLineWidth());

		return connection;
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.CONNECTION_ENDPOINTS_ROLE,
				new ConnectionEndpointEditPolicy());
	}

	public SankeyLink getModel() {
		return (SankeyLink) super.getModel();
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {}

}
