package org.openlca.app.results.analysis.sankey.edit;

import static org.eclipse.gef.LayerConstants.*;
import static org.openlca.app.results.analysis.sankey.SankeyConfig.*;

import java.beans.PropertyChangeEvent;
import java.util.List;

import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.draw2d.ConnectionRouter;
import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.editpolicies.RootComponentEditPolicy;
import org.openlca.app.results.analysis.sankey.layouts.SankeyLayout;
import org.openlca.app.results.analysis.sankey.model.Diagram;

public class DiagramEditPart extends AbstractComponentEditPart<Diagram> {

	/**
	 * Upon activation, attach to the GraphConfig element as a property change
	 * listener.
	 */
	@Override
	public void activate() {
		if (!isActive()) {
			super.activate();
			getModel().getConfig().addListener(this);
		}
	}

	/**
	 * Upon deactivation, detach from the GraphConfig element as a property change
	 * listener.
	 */
	@Override
	public void deactivate() {
		if (isActive()) {
			super.deactivate();
			getModel().getConfig().removeListener(this);
		}
	}

	@Override
	protected void createEditPolicies() {
		// Disallows the removal of this edit part.
		installEditPolicy(EditPolicy.COMPONENT_ROLE,
				new RootComponentEditPolicy());
		// Handles constraint changes (e.g. moving and/or resizing) of model
		// elements within the graph and creation of new model elements.
		installEditPolicy(EditPolicy.LAYOUT_ROLE, new SankeyXYLayoutEditPolicy());
	}

	@Override
	protected IFigure createFigure() {
		var theme = getModel().getEditor().getTheme();
		getViewer().getControl().setBackground(theme.backgroundColor());

		var f = new FreeformLayer();
		var layout = new SankeyLayout(getModel().editor, getModel().orientation);
		layout.setDistanceLevel(150);
		f.setLayoutManager(layout);
		return f;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		if (CONFIG_PROP.equals(prop)) {
			refresh();
		}
		else super.propertyChange(evt);
	}

	@Override
	protected void refreshVisuals() {
		var cLayer = (ConnectionLayer) getLayer(CONNECTION_LAYER);
		cLayer.setConnectionRouter(ConnectionRouter.NULL);

		super.refreshVisuals();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<SankeyNodeEditPart> getChildren() {
		return (List<SankeyNodeEditPart>) super.getChildren();
	}

	public String toString() {
		return "DiagramEditPart";
	}

}
