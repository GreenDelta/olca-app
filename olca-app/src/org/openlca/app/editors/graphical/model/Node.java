package org.openlca.app.editors.graphical.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;

abstract class Node implements Comparable<Node> {

	private Node parent;
	private List<Node> children = new ArrayList<>();
	public IFigure figure;
	AppAbstractEditPart<?> editPart;

	public Node parent() {
		return parent;
	}

	public boolean add(Node child) {
		if (!children.add(child))
			return false;
		child.parent = this;
		if (editPart != null)
			editPart.refreshChildren();
		return true;
	}

	public boolean remove(Node child) {
		if (!children.remove(child))
			return false;
		if (editPart != null)
			editPart.refreshChildren();
		return true;
	}

	public boolean contains(final Node child) {
		return children.contains(child);
	}

	public List<? extends Node> getChildren() {
		return children;
	}

	public abstract String getName();

	public boolean isVisible() {
		return figure != null ? figure.isVisible() : false;
	}

	public void setVisible(boolean value) {
		if (figure == null)
			return;
		figure.setVisible(value);
	}

	public Dimension getSize() {
		return figure != null ? figure.getSize() : new Dimension();
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