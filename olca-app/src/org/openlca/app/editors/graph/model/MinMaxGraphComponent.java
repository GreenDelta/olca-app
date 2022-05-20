package org.openlca.app.editors.graph.model;

import org.eclipse.draw2d.geometry.Dimension;
import org.openlca.app.editors.graph.GraphEditor;

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

	public abstract void addChildren();

	public void removeChildren() {
		var children = new ArrayList<>(getChildren());
		for (var child : children)
			removeChild(child);
	}

	public void updateLinks() {
		System.out.printf("Updating links of %s: %s\n", this, getAllLinks());
		for (Link link : getAllLinks()) {
			link.reconnect(link.getSource(), link.getTarget());
		}
	}

}
