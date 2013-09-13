/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.analysis.sankey;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

/**
 * Node is the representation of a model component
 * 
 * @author Sebastian Greve
 * 
 */
public abstract class Node implements Comparable<Node> {

	/**
	 * String for PropertyChangeEvent 'PROPERTY_ADD'
	 */
	public static String PROPERTY_ADD = "NodeAddChild";

	/**
	 * String for PropertyChangeEvent 'PROPERTY_ADD'
	 */
	public static String PROPERTY_LAYOUT = "Layout";

	/**
	 * String for PropertyChangeEvent 'PROPERTY_REMOVE'
	 */
	public static String PROPERTY_REMOVE = "NodeRemoveChild";

	/**
	 * List of children nodes
	 */
	private List<Node> children;

	/**
	 * Name of this node (will be shown in editor)
	 */
	private String name;

	/**
	 * Parent node
	 */
	private Node parent;

	/**
	 * {@link PropertyChangeSupport}
	 */
	protected PropertyChangeSupport listeners;

	/**
	 * Creates a new Node with name = 'Unknown'
	 */
	public Node() {
		name = "Unknown";
		children = new ArrayList<>();
		parent = null;
		listeners = new PropertyChangeSupport(this);
	}

	/**
	 * Adds a child node and sets this node as the parent of the child
	 * 
	 * @param child
	 *            - the child node which should be added
	 * @return true if the child could be added
	 */
	public boolean addChild(Node child) {
		boolean b = children.add(child);
		if (b) {
			child.setParent(this);
			listeners.firePropertyChange(PROPERTY_ADD, null, child);
		}
		return b;
	}

	/**
	 * Adds a {@link PropertyChangeListener} to the support
	 * 
	 * @param listener
	 *            The listener to be added
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		listeners.addPropertyChangeListener(listener);
	}

	@Override
	public int compareTo(Node o) {
		String s1 = getName().toLowerCase();
		String s2 = o.getName().toLowerCase();
		int length = s1.length();
		if (length > s2.length()) {
			length = s2.length();
		}
		for (int i = 0; i < length; i++) {
			if (s1.charAt(i) > s2.charAt(i)) {
				return 1;
			} else if (s1.charAt(i) < s2.charAt(i)) {
				return -1;
			}
		}
		return 0;
	}

	/**
	 * Checks if the children contain the given Node
	 * 
	 * @param child
	 *            The child to check
	 * @return true if children contains the given Node
	 */
	public boolean contains(Node child) {
		return children.contains(child);
	}

	/**
	 * Disposes this node
	 */
	public abstract void dispose();

	/**
	 * Getter of {@link #children}
	 * 
	 * @return List of children nodes
	 */
	public List<Node> getChildrenArray() {
		return children;
	}

	/**
	 * Getter of {@link #name}
	 * 
	 * @return the name of this node
	 */
	public String getName() {
		return name;
	}

	/**
	 * Getter of the parent node
	 * 
	 * @return The parent node
	 */
	public Node getParent() {
		return parent;
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		listeners.removePropertyChangeListener(listener);
	}

	public void setParent(Node parent) {
		this.parent = parent;
	}

}
