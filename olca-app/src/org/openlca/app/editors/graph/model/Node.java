package org.openlca.app.editors.graph.model;

import org.eclipse.draw2d.geometry.Dimension;

import java.util.ArrayList;
import java.util.List;

public class Node extends ConnectableModelElement {

	public static final String MINIMIZED_PROP = "minimized";

	private static final Dimension DEFAULT_SIZE = new Dimension(250, 34);

	private boolean minimized = true;
	private String name;

	public Node(String name) {
		size.width = DEFAULT_SIZE.width;
		size.height = DEFAULT_SIZE.height;
		location.x = 20;
		location.y = 20;
		this.name = name;
		System.out.println("Creating " + this);
	}

	public boolean isMinimized() {
		return minimized;
	}

	public void setMinimized(boolean value) {
		if (minimized == value)
			return;
		minimized = value;
		firePropertyChange(MINIMIZED_PROP, null, minimized); //$NON-NLS-1$
	}

	public String toString() {
		return "Node: " + name;
	}

}
