package org.openlca.app.editors.graph.edit;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.GraphicalEditPart;
import org.openlca.app.editors.graph.model.ConnectableModelElement;
import org.openlca.app.editors.graph.model.Node;

import java.beans.PropertyChangeEvent;

abstract class NodeEditPart extends AbstractGraphEditPart<Node>{

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.COMPONENT_ROLE, new NodeComponentEditPolicy());
		installEditPolicy(EditPolicy.GRAPHICAL_NODE_ROLE,
			new NodeGraphicalEditPolicy());
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		if (ConnectableModelElement.SIZE_PROP.equals(prop)
			|| ConnectableModelElement.LOCATION_PROP.equals(prop)) {
			refreshVisuals();
		} else if (ConnectableModelElement.INPUTS_PROP.equals(prop)) {
			refreshSourceConnections();
		} else if (ConnectableModelElement.OUTPUTS_PROP.equals(prop)) {
			refreshTargetConnections();
		}
	}

	@Override
	protected void refreshVisuals() {
		Rectangle bounds = new Rectangle(getModel().getLocation(),
			getModel().getSize());
		((GraphicalEditPart) getParent()).setLayoutConstraint(this,
			getFigure(), bounds);
	}

}
