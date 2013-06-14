/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.productsystem.graphical.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.IFigure;

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
	public static final String PROPERTY_ADD = "NodeAddChild";

	/**
	 * String for PropertyChangeEvent 'PROPERTY_LAYOUT'
	 */
	public static final String PROPERTY_LAYOUT = "NodeLayout";

	/**
	 * String for PropertyChangeEvent 'PROPERTY_REMOVE'
	 */
	public static final String PROPERTY_REMOVE = "NodeRemoveChild";

	/**
	 * List of children nodes
	 */
	private final List<Node> children;

	/**
	 * The figure of the node
	 */
	private IFigure figure;

	/**
	 * Name of this node (will be shown in editor)
	 */
	private final String name;

	/**
	 * Parent node
	 */
	private Node parent;

	/**
	 * {@link PropertyChangeSupport}
	 */
	private final PropertyChangeSupport support;

	/**
	 * Creates a new Node with name = 'Unknown'
	 */
	public Node() {
		name = "Unknown";
		children = new ArrayList<>();
		parent = null;
		support = new PropertyChangeSupport(this);
	}

	/**
	 * Adds a child node and sets this node as the parent of the child
	 * 
	 * @param child
	 *            the child node which should be added
	 * @return true if the child could be added
	 */
	public boolean addChild(final Node child) {
		final boolean b = children.add(child);
		if (b) {
			child.setParent(this);
			getSupport().firePropertyChange(PROPERTY_ADD, null, child);
		}
		return b;
	}

	/**
	 * Adds a property change listener to the support
	 * 
	 * @param listener
	 *            The listener the be added
	 */
	public void addPropertyChangeListener(final PropertyChangeListener listener) {
		support.addPropertyChangeListener(listener);
	}

	@Override
	public int compareTo(final Node o) {
		final String s1 = getName().toLowerCase();
		final String s2 = o.getName().toLowerCase();
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
	 *            The child that should be checked
	 * @return true if children contains the given Node
	 */
	public boolean contains(final Node child) {
		return children.contains(child);
	}

	/**
	 * Method to release the node
	 */
	public abstract void dispose();

	/**
	 * Getter of children-field
	 * 
	 * @return List of children nodes
	 */
	public List<Node> getChildrenArray() {
		return children;
	}

	/**
	 * Getter of figure-field
	 * 
	 * @return The figure of the node
	 */
	public IFigure getFigure() {
		return figure;
	}

	/**
	 * Getter of name-field
	 * 
	 * @return the name of the node
	 */
	public String getName() {
		return name;
	}

	/**
	 * Getter of parent-field
	 * 
	 * @return The parent node of the node
	 */
	public Node getParent() {
		return parent;
	}

	/**
	 * Getter of support-field
	 * 
	 * @return The property change support of the node
	 */
	public PropertyChangeSupport getSupport() {
		return support;
	}

	/**
	 * Removes the given child from children
	 * 
	 * @param child
	 *            The Node which has to be removed
	 * @return true if child could be removed
	 */
	public boolean removeChild(final Node child) {
		final boolean b = children.remove(child);
		if (b) {
			getSupport().firePropertyChange(PROPERTY_REMOVE, child, null);
		}
		return b;
	}

	/**
	 * Removes a property change listener from the support
	 * 
	 * @param listener
	 *            The listener the be removed
	 */
	public void removePropertyChangeListener(
			final PropertyChangeListener listener) {
		support.removePropertyChangeListener(listener);
	}

	/**
	 * Setter of figure-field
	 * 
	 * @param figure
	 *            The new figure of the node
	 */
	public void setFigure(final IFigure figure) {
		this.figure = figure;
	}

	/**
	 * Setter of parent-field
	 * 
	 * @param parent
	 *            The new parent of the node
	 */
	public void setParent(final Node parent) {
		this.parent = parent;
	}

}
