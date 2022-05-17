package org.openlca.app.editors.graph.model;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.openlca.app.editors.graph.GraphEditor;

import java.util.ArrayList;
import java.util.List;

abstract public class GraphComponent extends GraphElement {

	public static final String CHILDREN_PROP = "children", INPUTS_PROP = "inputs",
		OUTPUTS_PROP = "outputs", SIZE_PROP = "size", LOCATION_PROP = "location";

	protected List<GraphComponent> children = new ArrayList<>();
	private GraphComponent parent;

	protected Point location = new Point(0, 0);
	protected Dimension size = new Dimension(-1, -1);

	GraphComponent(GraphEditor editor) {
		super(editor);
	}


	public Point getLocation() {
		return location;
	}

	public Dimension getSize() {
		return size;
	}

	public void setLocation(Point p) {
		if (location.equals(p))
			return;
		location = p;
		firePropertyChange(LOCATION_PROP, null, location);
	}

	public void setSize(Dimension d) {
		if (size.equals(d))
			return;
		size = d;
		firePropertyChange(SIZE_PROP, null, size);
	}

	protected void fireStructureChange(String prop, Object child) {
		firePropertyChange(prop, null, child);
	}

	public void addChild(GraphComponent child) {
		addChild(child, -1);
	}

	public void addChild(GraphComponent child, int index) {
		if (index >= 0)
			children.add(index, child);
		else
			children.add(child);
		child.setParent(this);
		firePropertyChange(CHILDREN_PROP, index, child);
	}

	public void setParent(GraphComponent parent) {
		this.parent = parent;
	}

	public GraphComponent getParent() {
		return parent;
	}

	public void removeChild(GraphComponent child) {
		children.remove(child);
		firePropertyChange(CHILDREN_PROP, child, null);
	}

	public List<? extends GraphComponent> getChildren() {
		return children;
	}

}
