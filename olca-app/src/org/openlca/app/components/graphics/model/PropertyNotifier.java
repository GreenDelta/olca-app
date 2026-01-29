package org.openlca.app.components.graphics.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/// A base class for our graph model for notifying property changes. Currently,
/// the communication between the graph models and views are done via property
/// change events and this class provides basic support for this.
abstract public class PropertyNotifier {

	private final PropertyChangeSupport listeners = new PropertyChangeSupport(this);

	public void addListener(PropertyChangeListener l) {
		listeners.addPropertyChangeListener(l);
	}

	public void removeListener(PropertyChangeListener l) {
		listeners.removePropertyChangeListener(l);
	}

	/// Notifies the listeners of this object that a property changed.
	///
	/// @param prop     the name of the property
	/// @param oldValue the old value
	/// @param newValue the new value
	public void notifyChange(String prop, Object oldValue, Object newValue) {
		listeners.firePropertyChange(prop, oldValue, newValue);
	}

	/// Notifies the listeners of this object that a property changed.
	public void notifyChange(String prop) {
		listeners.firePropertyChange(prop, null, null);
	}

}
