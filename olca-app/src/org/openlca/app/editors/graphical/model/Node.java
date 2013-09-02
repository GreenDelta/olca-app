/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.editors.graphical.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;

public abstract class Node implements Comparable<Node> {

	public static final String PROPERTY_ADD = "NodeAddChild";
	public static final String PROPERTY_LAYOUT = "NodeLayout";
	public static final String PROPERTY_REMOVE = "NodeRemoveChild";

	private Node parent;
	private List<Node> children;
	private IFigure figure;
	private String name;
	private PropertyChangeSupport support;

	public Node() {
		name = "Unknown";
		children = new ArrayList<>();
		parent = null;
		support = new PropertyChangeSupport(this);
	}

	public Node getParent() {
		return parent;
	}

	public void setParent(Node parent) {
		this.parent = parent;
	}

	protected boolean add(Node child) {
		boolean b = children.add(child);
		if (b) {
			child.setParent(this);
			getSupport().firePropertyChange(PROPERTY_ADD, null, child);
		}
		return b;
	}

	protected boolean remove(Node child) {
		boolean b = children.remove(child);
		if (b)
			getSupport().firePropertyChange(PROPERTY_REMOVE, child, null);
		return b;
	}

	public boolean contains(final Node child) {
		return children.contains(child);
	}

	public List<? extends Node> getChildren() {
		return children;
	}

	public IFigure getFigure() {
		return figure;
	}

	protected void setFigure(IFigure figure) {
		this.figure = figure;
	}

	public String getName() {
		return name;
	}

	public boolean isVisible() {
		return getFigure() != null ? getFigure().isVisible() : false;
	}

	public void setVisible(boolean value) {
		if (getFigure() != null)
			getFigure().setVisible(value);
	}

	public Dimension getSize() {
		return getFigure() != null ? getFigure().getSize() : new Dimension();
	}

	public PropertyChangeSupport getSupport() {
		return support;
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		getSupport().addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		getSupport().removePropertyChangeListener(listener);
	}

	@Override
	public int compareTo(final Node o) {
		final String s1 = getName().toLowerCase();
		final String s2 = o.getName().toLowerCase();
		int length = s1.length();
		if (length > s2.length())
			length = s2.length();
		for (int i = 0; i < length; i++)
			if (s1.charAt(i) > s2.charAt(i))
				return 1;
			else if (s1.charAt(i) < s2.charAt(i))
				return -1;
		return 0;
	}

}
