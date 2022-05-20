package org.openlca.app.editors.graph.model;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.openlca.app.editors.graph.GraphConfig;
import org.openlca.app.editors.graph.GraphEditor;

import java.util.ArrayList;
import java.util.List;

abstract public class GraphComponent extends GraphElement {

	public static final String CHILDREN_PROP = "children", TARGET_CONNECTIONS_PROP = "targets",
		SOURCE_CONNECTIONS_PROP = "sources", SIZE_PROP = "size", LOCATION_PROP = "location";

	public final GraphEditor editor;

	protected List<GraphComponent> children = new ArrayList<>();
	private GraphComponent parent;

	private List<Link> sourceConnections = new ArrayList<>();
	private List<Link> targetConnections = new ArrayList<>();

	protected Point location = new Point(0, 0);
	protected Dimension size = new Dimension(-1, -1);

	GraphComponent(GraphEditor editor) {
		this.editor = editor;
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

	public GraphConfig getConfig() {
		return editor.config;
	}

	public void removeChild(GraphComponent child) {
		children.remove(child);
		firePropertyChange(CHILDREN_PROP, child, null);
	}

	public List<? extends GraphComponent> getChildren() {
		return children;
	}

	void addConnection(Link link) {
		if (link == null || targetConnections.contains(link) || sourceConnections.contains(link))
			return;
		if (link.getTarget() == this) {
			targetConnections.add(link);
			firePropertyChange(TARGET_CONNECTIONS_PROP, null, link);
		} else if (link.getSource() == this) {
			sourceConnections.add(link);
			firePropertyChange(SOURCE_CONNECTIONS_PROP, null, link);
		}
	}

	void removeConnection(Link link) {
		if (link == null) {
			return;
		}
		if (link.getTarget() == this) {
			targetConnections.remove(link);
			firePropertyChange(TARGET_CONNECTIONS_PROP, link, null);
		}
		else if (link.getSource() == this) {
			sourceConnections.remove(link);
			firePropertyChange(SOURCE_CONNECTIONS_PROP, link, null);
		}
	}

	public List<Link> getTargetConnections() {
		return targetConnections;
	}

	public List<Link> getSourceConnections() {
		return sourceConnections;
	}

	/**
	 * Retrieve the links from this and its children.
	 */
	public List<Link> getAllLinks() {
		List<Link> links = new ArrayList<>();
		links.addAll(getTargetConnections());
		links.addAll(getSourceConnections());
		for (var child : getChildren()) {
			links.addAll(child.getAllLinks());
		}
		return links;
	}

}
