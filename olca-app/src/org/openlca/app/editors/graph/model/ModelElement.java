package org.openlca.app.editors.graph.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;

/**
 * Abstract prototype of a model element.
 * <p>
 * This class provides features necessary for all model elements, like:
 * </p>
 * <ul>
 * <li>property-change support (used to notify edit parts of model changes),</li>
 * <li>...</li>
 */
abstract public class ModelElement {

	protected PropertyChangeSupport listeners = new PropertyChangeSupport(this);


	public void addPropertyChangeListener(PropertyChangeListener l) {
		System.out.println("Adding property listener " + l);
		listeners.addPropertyChangeListener(l);
	}

	public void removePropertyChangeListener(PropertyChangeListener l) {
		listeners.removePropertyChangeListener(l);
	}

	/**
	 * Reports a bound property update to listeners that have been registered to
	 * track updates of all properties or a property with the specified name.
	 * <p>
	 * No event is fired if old and new values are equal and non-null.
	 * <p>
	 *
	 * @param prop          the programmatic name of the property that was changed
	 * @param oldValue      the old value of the property
	 * @param newValue      the new value of the property
	 */
	protected void firePropertyChange(String prop, Object oldValue,
																		Object newValue) {
		System.out.printf("Firing %s property change for %s", prop, Arrays.toString(listeners.getPropertyChangeListeners()));
		listeners.firePropertyChange(prop, oldValue, newValue);
	}

	public void update() {
	}

}
