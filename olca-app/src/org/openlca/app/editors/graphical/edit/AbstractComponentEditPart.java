package org.openlca.app.editors.graphical.edit;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.openlca.app.editors.graphical.model.GraphComponent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import static org.openlca.app.editors.graphical.model.GraphComponent.CHILDREN_PROP;

public abstract class AbstractComponentEditPart<N extends GraphComponent> extends
	AbstractGraphicalEditPart implements PropertyChangeListener {

	/**
	 * Upon activation, attach to the model element as a property change
	 * listener.
	 */
	@Override
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
	@Override
	public void deactivate() {
		if (isActive()) {
			super.deactivate();
			getModel().removePropertyChangeListener(this);
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		if (CHILDREN_PROP.equals(prop)) {
			refreshChildren();
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

	/**
	 * Reset a child by cloning, removing and adding a new version of it. This
	 * method is useful when, for the same model class, the EditPart class
	 * differs.
	 *
	 * @param childEditPart The child EditPart to be reset.
	 */
	public void resetChildEditPart(EditPart childEditPart) {
		var index = getChildren().indexOf(childEditPart);
		var newNodeEditPart = createChild(childEditPart.getModel());
		removeChild(childEditPart);
		addChild(newNodeEditPart, index);
	}
}
