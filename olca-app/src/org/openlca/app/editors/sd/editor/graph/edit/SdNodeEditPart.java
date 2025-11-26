package org.openlca.app.editors.sd.editor.graph.edit;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.openlca.app.components.graphics.model.Component;
import org.openlca.app.editors.sd.editor.graph.figures.AuxiliaryFigure;
import org.openlca.app.editors.sd.editor.graph.figures.RateFigure;
import org.openlca.app.editors.sd.editor.graph.figures.StockFigure;
import org.openlca.app.editors.sd.editor.graph.model.SdLink;
import org.openlca.app.editors.sd.editor.graph.model.SdNode;

/**
 * EditPart for SdNode model elements.
 * Handles stocks, rates, and auxiliaries.
 */
public class SdNodeEditPart extends AbstractGraphicalEditPart
		implements PropertyChangeListener, NodeEditPart {

	private ConnectionAnchor anchor;

	@Override
	public void activate() {
		if (!isActive()) {
			super.activate();
			getModel().addPropertyChangeListener(this);
		}
	}

	@Override
	public void deactivate() {
		if (isActive()) {
			getModel().removePropertyChangeListener(this);
			super.deactivate();
		}
	}

	@Override
	protected IFigure createFigure() {
		var node = getModel();
		return switch (node.getType()) {
			case STOCK -> new StockFigure(node);
			case RATE -> new RateFigure(node);
			case AUXILIARY -> new AuxiliaryFigure(node);
		};
	}

	@Override
	protected void createEditPolicies() {
		// Allow deletion
		installEditPolicy(EditPolicy.COMPONENT_ROLE, new SdNodeEditPolicy());
		// Allow connections
		installEditPolicy(EditPolicy.GRAPHICAL_NODE_ROLE, new SdNodeGraphicalEditPolicy());
	}

	@Override
	public SdNode getModel() {
		return (SdNode) super.getModel();
	}

	@Override
	protected void refreshVisuals() {
		var figure = getFigure();
		var node = getModel();
		var parent = (GraphicalEditPart) getParent();

		var location = node.getLocation();
		var size = node.getSize();
		var bounds = new Rectangle(location, size);
		parent.setLayoutConstraint(this, figure, bounds);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		if (Component.SIZE_PROP.equals(prop) || Component.LOCATION_PROP.equals(prop)) {
			refreshVisuals();
		} else if (SdNode.NAME_PROP.equals(prop)) {
			refreshVisuals();
			// Update the figure label
			var figure = getFigure();
			if (figure instanceof StockFigure sf) {
				sf.updateName();
			} else if (figure instanceof RateFigure rf) {
				rf.updateName();
			} else if (figure instanceof AuxiliaryFigure af) {
				af.updateName();
			}
		} else if (Component.SOURCE_CONNECTIONS_PROP.equals(prop)) {
			refreshSourceConnections();
		} else if (Component.TARGET_CONNECTIONS_PROP.equals(prop)) {
			refreshTargetConnections();
		}
	}

	@Override
	protected List<SdLink> getModelSourceConnections() {
		var links = new ArrayList<SdLink>();
		for (var link : getModel().getSourceConnections()) {
			if (link instanceof SdLink sdLink) {
				links.add(sdLink);
			}
		}
		return links;
	}

	@Override
	protected List<SdLink> getModelTargetConnections() {
		var links = new ArrayList<SdLink>();
		for (var link : getModel().getTargetConnections()) {
			if (link instanceof SdLink sdLink) {
				links.add(sdLink);
			}
		}
		return links;
	}

	protected ConnectionAnchor getConnectionAnchor() {
		if (anchor == null) {
			anchor = new ChopboxAnchor(getFigure());
		}
		return anchor;
	}

	@Override
	public ConnectionAnchor getSourceConnectionAnchor(ConnectionEditPart connection) {
		return getConnectionAnchor();
	}

	@Override
	public ConnectionAnchor getTargetConnectionAnchor(ConnectionEditPart connection) {
		return getConnectionAnchor();
	}

	@Override
	public ConnectionAnchor getSourceConnectionAnchor(Request request) {
		return getConnectionAnchor();
	}

	@Override
	public ConnectionAnchor getTargetConnectionAnchor(Request request) {
		return getConnectionAnchor();
	}
}
