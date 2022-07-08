package org.openlca.app.editors.graphical.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Abstract prototype of a model element.
 * <p>
 * This class provides features necessary for all model elements:
 * </p>
 * <ul>
 * <li>property-change support (used to notify edit parts of model changes),</li>
 * </ul>
 */
abstract public class GraphElement {

	protected PropertyChangeSupport listeners = new PropertyChangeSupport(this);

	public void addPropertyChangeListener(PropertyChangeListener l) {
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
		listeners.firePropertyChange(prop, oldValue, newValue);
	}

}
