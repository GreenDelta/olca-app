package org.openlca.app.tools.graphics.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.swt.SWT;
import org.openlca.app.editors.graphical.model.Node;

/**
 * Abstract prototype of a model component.
 * <p>
	 * This class provides features necessary for all model components:
	 * </p>
	 * <ul>
	 * <li>management of size and location,</li>
	 * <li>support for adding and removing children,</li>
	 * <li>methods for connections with other <code>Components</code>,</li>
 */
abstract public class Component extends Element implements Comparable<Component> {

	public static final String
		CHILDREN_PROP = "children",
		TARGET_CONNECTIONS_PROP = "targets",
		SOURCE_CONNECTIONS_PROP = "sources",
		SIZE_PROP = "size",
		LOCATION_PROP = "location";

	protected List<Component> children = new ArrayList<>();
	private Component parent;

	private final List<Link> sourceConnections = new ArrayList<>();
	private final List<Link> targetConnections = new ArrayList<>();

	protected Point location;
	protected Dimension size = new Dimension(SWT.DEFAULT, SWT.DEFAULT);

	public Point getLocation() {
		return location;
	}

	public Dimension getSize() {
		return size;
	}

	public void setLocation(Point p) {
		if (p == null || Objects.equals(location, p))
			return;
		location = p;
		firePropertyChange(LOCATION_PROP, null, location);
	}

	public void setSize(Dimension d) {
		if (d == null || size.equals(d))
			return;
		size = d;
		firePropertyChange(SIZE_PROP, null, size);
	}

	public void addChild(Component child) {
		addChild(child, -1);
	}

	public void addChild(Component child, int index) {
		if (index >= 0)
			children.add(index, child);
		else
			children.add(child);
		child.setParent(this);
		firePropertyChange(CHILDREN_PROP, index, child);
	}

	public void addChildren(List<Node> children) {
		for (var child : children) {
			children.add(child);
			child.setParent(this);
		}
		firePropertyChange(CHILDREN_PROP, null, null);
	}

	public void setParent(Component parent) {
		this.parent = parent;
	}

	public Component getParent() {
		return parent;
	}

	/**
	 * Remove a child from this.
	 *
	 * @param child
	 *            a non-null component instance;
	 * @return true, if the component was removed, false otherwise
	 */
	public boolean removeChild(Component child) {
		if (child != null && children.remove(child)) {
			firePropertyChange(CHILDREN_PROP, child, null);
			return true;
		}
		return false;
	}

	public void removeAllChildren() {
		var children = new ArrayList<>(getChildren());
		for (var child : children) removeChild(child);
	}

	public List<? extends Component> getChildren() {
		return children;
	}

	public void addConnection(Link link) {
		if (link == null)
			return;
		if (link.getTarget() == this && !targetConnections.contains(link)) {
			targetConnections.add(link);
			firePropertyChange(TARGET_CONNECTIONS_PROP, null, link);
		} else if (link.getSource() == this && !sourceConnections.contains(link)) {
			sourceConnections.add(link);
			firePropertyChange(SOURCE_CONNECTIONS_PROP, null, link);
		}
	}

	public void removeConnection(Link link) {
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
		return new ArrayList<>(targetConnections);
	}

	public List<Link> getSourceConnections() {
		return new ArrayList<>(sourceConnections);
	}

	/**
	 * Create a list of Links with the target connections of this, its children
	 * and grandchildren to get all target connections. If this is an
	 * ExchangeItem, it simply returns its target connections.
	 * @return List of all the target connections.
	 */
	public List<Link> getAllTargetConnections() {
		var links = new LinkedHashSet<>(getTargetConnections());
		for (var child : getChildren()) {
			links.addAll(child.getAllTargetConnections());
		}
		return new ArrayList<>(links);
	}

	/**
	 * Create a list of Links with the source connections of this, its children
	 * and grandchildren to get all source connections. If this is an
	 * ExchangeItem, it simply returns its source connections.
	 * @return List of all the source connections.
	 */
	public List<Link> getAllSourceConnections() {
		var links = new LinkedHashSet<>(getSourceConnections());
		for (var child : getChildren()) {
			links.addAll(child.getAllSourceConnections());
		}
		return new ArrayList<>(links);
	}

	/**
	 * Retrieve the links from this and its children.
	 */
	public List<Link> getAllLinks() {
		var links = new LinkedHashSet<>(getAllSourceConnections());
		links.addAll(getAllTargetConnections());
		return new ArrayList<>(links);
	}

	public List<Component> getSiblings(boolean forInputs) {
		var links = forInputs
				? getAllTargetConnections()
				: getAllSourceConnections();
		return links.stream()
				.map((forInputs) ? Link::getSourceNode : Link::getTargetNode)
				.toList();
	}

	@Override
	public abstract int compareTo(Component other);

	public String getComparisonLabel() {
		return "";
	}

}
