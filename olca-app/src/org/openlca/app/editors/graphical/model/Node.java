package org.openlca.app.editors.graphical.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;

abstract class Node implements Comparable<Node> {

	private Node parent;
	private List<Node> children;
	private IFigure figure;
	private AppAbstractEditPart<?> editPart;

	Node() {
		children = new ArrayList<>();
		parent = null;
	}

	void setEditPart(AppAbstractEditPart<?> editPart) {
		this.editPart = editPart;
	}

	AbstractGraphicalEditPart getEditPart() {
		return editPart;
	}

	public Node getParent() {
		return parent;
	}

	public void setParent(Node parent) {
		this.parent = parent;
	}

	boolean add(Node child) {
		boolean b = children.add(child);
		if (b) {
			child.setParent(this);
			if (editPart != null)
				editPart.refreshChildren();
		}
		return b;
	}

	boolean remove(Node child) {
		boolean b = children.remove(child);
		if (b)
			if (editPart != null)
				editPart.refreshChildren();
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

	void setFigure(IFigure figure) {
		this.figure = figure;
	}

	public abstract String getName();

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