package org.openlca.app.editors.graph.model;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;

import java.util.ArrayList;
import java.util.List;

abstract public class ConnectableModelElement extends ModelElement {

	public static final String CHILDREN_PROP = "children", INPUTS_PROP = "inputs",
		OUTPUTS_PROP = "outputs", SIZE_PROP = "size", LOCATION_PROP = "location";

	protected List<ConnectableModelElement> children = new ArrayList<>();

	protected Point location = new Point(0, 0);
	protected Dimension size = new Dimension(-1, -1);


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
		firePropertyChange(LOCATION_PROP, null, location); //$NON-NLS-1$
	}

	public void setSize(Dimension d) {
		if (size.equals(d))
			return;
		size = d;
		firePropertyChange(SIZE_PROP, null, size); //$NON-NLS-1$
	}

	protected void fireStructureChange(String prop, Object child) {
		firePropertyChange(prop, null, child);
	}

	public void addChild(ConnectableModelElement child) {
		addChild(child, -1);
	}

	public void addChild(ConnectableModelElement child, int index) {
		if (index >= 0)
			children.add(index, child);
		else
			children.add(child);
		firePropertyChange(CHILDREN_PROP, index, child);
	}

	public void removeChild(ConnectableModelElement child) {
		children.remove(child);
		firePropertyChange(CHILDREN_PROP, child, null);
	}

	public List<? extends ConnectableModelElement> getChildren() {
		return children;
	}

}
