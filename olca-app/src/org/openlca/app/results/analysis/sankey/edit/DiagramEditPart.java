package org.openlca.app.results.analysis.sankey.edit;

import org.eclipse.draw2d.*;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.editpolicies.RootComponentEditPolicy;
import org.openlca.app.results.analysis.sankey.layouts.SankeyLayout;
import org.openlca.app.results.analysis.sankey.layouts.TreeConnectionRouter;
import org.openlca.app.results.analysis.sankey.model.Diagram;

import java.beans.PropertyChangeEvent;
import java.util.List;
import java.util.Objects;

import static org.eclipse.gef.LayerConstants.CONNECTION_LAYER;
import static org.openlca.app.results.analysis.sankey.SankeyConfig.CONFIG_PROP;
import static org.openlca.app.results.analysis.sankey.SankeyConfig.ROUTER_MANHATTAN;

public class DiagramEditPart extends AbstractComponentEditPart<Diagram> {

	/**
	 * Upon activation, attach to the GraphConfig element as a property change
	 * listener.
	 */
	@Override
	public void activate() {
		if (!isActive()) {
			super.activate();
			getModel().getConfig().addPropertyChangeListener(this);
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
			getModel().getConfig().removePropertyChangeListener(this);
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
		var theme = getModel().getConfig().getTheme();
		getViewer().getControl().setBackground(theme.backgroundColor());

		var f = new FreeformLayer();
		f.setBorder(new MarginBorder(8000));
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
		var router = getModel().getConfig().connectionRouter();

		if (Objects.equals(router, ROUTER_MANHATTAN))
			cLayer.setConnectionRouter(new TreeConnectionRouter());
		else cLayer.setConnectionRouter(ConnectionRouter.NULL);

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
