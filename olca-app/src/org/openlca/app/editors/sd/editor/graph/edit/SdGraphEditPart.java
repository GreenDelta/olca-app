package org.openlca.app.editors.sd.editor.graph.edit;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.draw2d.ConnectionRouter;
import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.FreeformLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.openlca.app.editors.sd.editor.graph.model.SdGraph;
import org.openlca.app.editors.sd.editor.graph.model.SdNode;
import org.openlca.app.util.Colors;

/**
 * EditPart for the SdGraph model.
 * This is the root edit part that contains all node edit parts.
 */
public class SdGraphEditPart extends AbstractGraphicalEditPart
		implements PropertyChangeListener {

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
		var figure = new FreeformLayer();
		figure.setBackgroundColor(Colors.white());
		figure.setOpaque(true);
		figure.setLayoutManager(new FreeformLayout());
		return figure;
	}

	@Override
	protected void createEditPolicies() {
		// Allow creating/deleting nodes and moving them
		installEditPolicy(EditPolicy.LAYOUT_ROLE, new SdGraphXYLayoutEditPolicy());
	}

	@Override
	public SdGraph getModel() {
		return (SdGraph) super.getModel();
	}

	@Override
	protected List<SdNode> getModelChildren() {
		return getModel().getNodes();
	}

	@Override
	protected void refreshVisuals() {
		// Set up connection router for the connection layer
		var connectionLayer = (ConnectionLayer) getLayer(LayerConstants.CONNECTION_LAYER);
		if (connectionLayer != null) {
			// Use null router for direct point-to-point connections
			connectionLayer.setConnectionRouter(ConnectionRouter.NULL);
		}
		super.refreshVisuals();
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		if (SdGraph.CHILDREN_PROP.equals(prop)) {
			refreshChildren();
		}
	}
}
