package org.openlca.app.editors.graph.model;

import org.eclipse.draw2d.geometry.Dimension;

public abstract class MinMaxGraphComponent extends GraphComponent {

	private boolean minimized = false;

	public boolean isMinimized() {
		return minimized;
	}

	/**
	 * This method does not implement a notification for the listener. Any change
	 * of <code>minimized</code> have to go through the
	 * <code>MinMaxCommand</code>.
	 */
	public void setMinimized(boolean value) {
		if (minimized == value)
			return;
		minimized = value;
		setSize(value ? getMinimizedSize() : getMaximizedSize());
	}

	protected abstract Dimension getMinimizedSize();

	protected abstract Dimension getMaximizedSize();

}
