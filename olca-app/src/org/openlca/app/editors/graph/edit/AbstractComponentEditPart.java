package org.openlca.app.editors.graph.edit;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.openlca.app.editors.graph.model.GraphComponent;

import java.beans.PropertyChangeListener;
import java.util.List;

public abstract class AbstractComponentEditPart<N extends GraphComponent> extends
	AbstractGraphicalEditPart implements PropertyChangeListener {

	/**
	 * Upon activation, attach to the model element as a property change
	 * listener.
	 */
	public void activate() {
		if (!isActive()) {
			super.activate();
			getModel().addPropertyChangeListener(this);
		}
	}

	/**
	 * Upon deactivation, detach from the model element as a property change
	 * listener.
	 */
	public void deactivate() {
		if (isActive()) {
			super.deactivate();
			getModel().removePropertyChangeListener(this);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public N getModel() {
		return (N) super.getModel();
	}

	@Override
	protected List<? extends GraphComponent> getModelChildren() {
		return getModel().getChildren();
	}

	public void resetChildEditPart(EditPart childEditPart) {
		var index = getChildren().indexOf(childEditPart);
		var newNodeEditPart = createChild(childEditPart.getModel());
		removeChild(childEditPart);
		addChild(newNodeEditPart, index);
	}
}
