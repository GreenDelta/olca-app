package org.openlca.app.results.analysis.sankey.edit;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;

import org.eclipse.draw2d.*;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.gef.editpolicies.ConnectionEndpointEditPolicy;
import org.eclipse.swt.graphics.Color;
import org.openlca.app.results.analysis.sankey.model.SankeyLink;
import org.openlca.app.tools.graphics.figures.Connection;

import static org.openlca.app.tools.graphics.figures.Connection.ROUTER_CURVE;

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
		var config = getModel().getSourceNode().getDiagram().getConfig();
		var theme = config.getTheme();

		Color color;
		if (getModel().getSourceNode().totalResult < 0)
			color = ColorConstants.green;
		else if (getModel().getSourceNode().totalResult > 0)
			color = ColorConstants.red;
		else color = ColorConstants.blue;
		var colorSelected = theme != null
				? theme.linkColorSelected()
				: ColorConstants.black;

		var orientation = getModel().getSourceNode().getDiagram().orientation;

		var connection = new Connection(config.connectionRouter(), orientation, color, colorSelected) {
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
