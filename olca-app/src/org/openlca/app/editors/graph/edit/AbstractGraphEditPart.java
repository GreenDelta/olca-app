package org.openlca.app.editors.graph.edit;

import java.beans.PropertyChangeListener;
import java.util.List;

import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.openlca.app.editors.graph.model.ConnectableModelElement;


abstract class AbstractGraphEditPart<N extends ConnectableModelElement> extends
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
	protected final List<? extends ConnectableModelElement> getModelChildren() {
		return getModel().getChildren();
	}
}
