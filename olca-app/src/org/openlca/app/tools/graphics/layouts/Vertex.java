package org.openlca.app.tools.graphics.layouts;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.openlca.app.tools.graphics.model.Component;

public class Vertex {

	final Dimension size;
	protected final Component node;
	final Figure figure;

	Vertex parent;
	final List<Vertex> children = new ArrayList<>();
	public int siblingIndex;
	Vertex previousSibling;

	public double prelim;
	public double modifier;

	public Vertex thread;
	public Vertex ancestor;
	public double number;
	public double change;
	public double shift;

	/** A mistletoe is a vertex that has more than one parent. This abnormal
	 * tree is laid out after the main tree.
	 */
	public TreeLayout mistletoe;

	public Point startLocation;
	public Point endLocation;

	public Vertex(Component node, Figure figure, Dimension size,
		int siblingIndex) {
		this.node = node;
		this.figure = figure;
		this.siblingIndex = siblingIndex;
		this.size = size;
	}

	public void addChild(Vertex childVertex) {
		children.add(childVertex);
	}

	public int getChildCount() {
		return children.size();
	}

	public void setParent(Vertex parent) {
		this.parent = parent;
	}

	public void setPreviousSibling(Vertex vertex) {
		previousSibling = vertex;
	}

	public Vertex getNextSibling() {
		if (parent == null) return null;
		return (parent.getLastChild() == this)
				? null
				: parent.children.get(siblingIndex + 1);
	}

	public Point getStartLocation() {
		return startLocation;
	}

	public Point getEndLocation() {
		return endLocation;
	}

	public Point getLocation() {
		if (endLocation == null)
			return null;
		else return endLocation.getTranslated(size.getScaled(0.5).getNegated());
	}

	public Vertex getFirstChild() {
		return (children.isEmpty()) ? null : children.get(0);
	}

	public Vertex getLastChild() {
		return (children.isEmpty()) ? null : children.get(children.size() - 1);
	}

	public void setLocation(Point location, Vertex parent) {
		endLocation = location;
		if (startLocation == null)
			startLocation = parent != null
					? parent.getStartLocation()
					: location;
	}

	public void setStartLocation(Point location) {
		startLocation = location;
	}

	public void clear() {
		children.clear();
		siblingIndex = 0;
		previousSibling = null;
		prelim = 0.0;
		modifier = 0.0;
		thread = null;
		ancestor = null;
		number = 0.0;
		change = 0.0;
		shift	= 0.0;
	}

	public String toString() {
		return node.getLabel();
	}

}
