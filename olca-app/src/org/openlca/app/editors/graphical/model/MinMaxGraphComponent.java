package org.openlca.app.editors.graphical.model;

import org.eclipse.draw2d.geometry.Dimension;
import org.openlca.app.editors.graphical.GraphEditor;

import java.util.ArrayList;

public abstract class MinMaxGraphComponent extends GraphComponent {

	private boolean minimized = true;

	MinMaxGraphComponent(GraphEditor editor) {
		super(editor);
	}

	public boolean isMinimized() {
		return minimized;
	}

	/**
	 * This method does not implement a notification for the listener as any
	 * changes of <code>minimized</code> have to be done ahead of figure painting
	 * (via <code>GraphFactory</code>) or through the <code>MinMaxCommand</code>.
	 */
	public void setMinimized(boolean value) {
		if (minimized == value)
			return;
		minimized = value;
		setSize(value ? getMinimizedSize() : getMaximizedSize());

	}

	protected abstract Dimension getMinimizedSize();

	protected abstract Dimension getMaximizedSize();

	public abstract void addChildren();

	public void reconnectLinks() {
		for (Link link : getAllLinks()) {
			link.reconnect(link.getSource(), link.getTarget());
		}
	}

}
