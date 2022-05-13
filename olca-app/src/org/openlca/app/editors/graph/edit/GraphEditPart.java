package org.openlca.app.editors.graph.edit;

import org.eclipse.draw2d.*;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.editpolicies.RootComponentEditPolicy;
import org.openlca.app.editors.graph.model.GraphComponent;
import org.openlca.app.editors.graph.model.Graph;

import java.beans.PropertyChangeEvent;

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
public class GraphEditPart extends AbstractComponentEditPart<Graph> {

		protected void createEditPolicies() {
			// Disallows the removal of this edit part.
			installEditPolicy(EditPolicy.COMPONENT_ROLE,
				new RootComponentEditPolicy());
			// Handles constraint changes (e.g. moving and/or resizing) of model
			// elements within the graph and creation of new model elements.
			installEditPolicy(EditPolicy.LAYOUT_ROLE, new GraphXYLayoutEditPolicy());
		}

		protected IFigure createFigure() {
			Figure f = new FreeformLayer();
			f.setBorder(new MarginBorder(3));
			f.setLayoutManager(new FreeformLayout());
			return f;
		}

	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		if (GraphComponent.CHILDREN_PROP.equals(prop))
			refreshChildren();
		}

}
