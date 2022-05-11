package org.openlca.app.editors.graph.edit;

import org.eclipse.draw2d.*;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.editpolicies.RootComponentEditPolicy;
import org.openlca.app.editors.graph.model.ConnectableModelElement;
import org.openlca.app.editors.graph.model.GraphModel;
import org.openlca.app.editors.graph.model.ModelElement;
import org.openlca.app.editors.graph.model.Node;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

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
public class GraphEditPart extends AbstractGraphicalEditPart implements
	PropertyChangeListener {

		/**
		 * Upon activation, attach to the model element as a property change
		 * listener.
		 */
		public void activate() {
			if (!isActive()) {
				super.activate();
				getGraphModel().addPropertyChangeListener(this);
			}
		}

	/**
	 * Upon deactivation, detach from the model element as a property change
	 * listener.
	 */
	public void deactivate() {
		if (isActive()) {
			super.deactivate();
			getGraphModel().removePropertyChangeListener(this);
		}
	}

		protected void createEditPolicies() {
			// Disallows the removal of this edit part.
			installEditPolicy(EditPolicy.COMPONENT_ROLE,
				new RootComponentEditPolicy());
			// Handles constraint changes (e.g. moving and/or resizing) of model
			// elements within the graph and creation of new model elements.
			installEditPolicy(EditPolicy.LAYOUT_ROLE, new GraphXYLayoutEditPolicy());
		}

		protected IFigure createFigure() {
		System.out.println("Creating GraphModel figure.");
			Figure f = new FreeformLayer();
			f.setBorder(new MarginBorder(3));
			f.setLayoutManager(new FreeformLayout());
			return f;
		}

	private GraphModel getGraphModel() {
			return (GraphModel) getModel();
		}

	@SuppressWarnings("unchecked")
	@Override
	protected List<Node> getModelChildren() {
		return (List<Node>) getGraphModel().getChildren();
	}

	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		System.out.println("In GraphEditPart.propertyChange with: " + prop);
		if (ConnectableModelElement.CHILDREN_PROP.equals(prop))
			refreshChildren();
		}

}
