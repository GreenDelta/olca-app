package org.openlca.app.editors.graphical.edit;

import static org.eclipse.gef.LayerConstants.*;
import static org.openlca.app.components.graphics.figures.Connection.*;
import static org.openlca.app.components.graphics.model.Component.*;
import static org.openlca.app.editors.graphical.GraphConfig.*;
import static org.openlca.app.editors.graphical.model.Graph.*;

import java.beans.PropertyChangeEvent;
import java.util.List;
import java.util.Objects;

import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.draw2d.ConnectionRouter;
import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPolicy;
import org.eclipse.swt.widgets.Canvas;
import org.openlca.app.editors.graphical.layouts.Layout;
import org.openlca.app.editors.graphical.layouts.TreeConnectionRouter;
import org.openlca.app.editors.graphical.model.Graph;

/**
 * EditPart for the GraphModel instance.
 * <p>
 * This edit part server as the main diagram container, the white area where
 * everything else is in. Also responsible for the container's layout (the way
 * the container rearranges is contents) and the container's capabilities (edit
 * policies).
 * </p>
 * <p>
 * This edit part must implement the PropertyChangeListener interface, so it can
 * be notified of property changes in the corresponding model element.
 * </p>
 */
public class GraphEditPart extends ComponentEditPart<Graph> {

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
		installEditPolicy(EditPolicy.COMPONENT_ROLE, new GraphEditPolicy());
		// Handles constraint changes (e.g. moving and/or resizing) of model
		// elements within the graph and creation of new model elements.
		installEditPolicy(EditPolicy.LAYOUT_ROLE, new GraphLayoutPolicy());
	}

	@Override
	protected IFigure createFigure() {
		var theme = getModel().getEditor().getTheme();

		// this could be better done when configuring the graphical
		// viewer in the graph editor, see the editor for system
		// dynamics models. We need to set the background color
		// with every paint event because it always falls back
		// to the default background color.
		var control = getViewer().getControl();
		control.setBackground(theme.backgroundColor());
		control.addPaintListener(e -> {
			if (!control.isDisposed()) {
				var current = getModel().getEditor().getTheme();
				control.setBackground(current.backgroundColor());
			}
		});

		var f = new FreeformLayer();
		f.setLayoutManager(new Layout(getModel().getEditor(), ORIENTATION));
		return f;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		if (CHILDREN_PROP.equals(prop))
			for (var node : getModel().getNodes())
				node.setButtonStatus();
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
	public List<NodeEditPart> getChildren() {
		return (List<NodeEditPart>) super.getChildren();
	}

	public NodeEditPart getReferenceNodeEditPart() {
		for (var child : getChildren())
			if (getModel().isReferenceProcess(child.getModel())) return child;
		return null;
	}

	public String toString() {
		return "GraphEditPart";
	}

}
