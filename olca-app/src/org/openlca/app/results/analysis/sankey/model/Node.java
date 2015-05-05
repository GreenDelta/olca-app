package org.openlca.app.results.analysis.sankey.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

public abstract class Node implements Comparable<Node> {

	public static String PROPERTY_ADD = "NodeAddChild";
	public static String PROPERTY_LAYOUT = "Layout";
	public static String PROPERTY_REMOVE = "NodeRemoveChild";

	private List<Node> children;
	private String name;
	private Node parent;

	protected PropertyChangeSupport listeners;

	public Node() {
		name = "Unknown";
		children = new ArrayList<>();
		parent = null;
		listeners = new PropertyChangeSupport(this);
	}

	public boolean addChild(Node child) {
		boolean b = children.add(child);
		if (b) {
			child.setParent(this);
			listeners.firePropertyChange(PROPERTY_ADD, null, child);
		}
		return b;
	}

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

	public boolean contains(Node child) {
		return children.contains(child);
	}

	public abstract void dispose();

	public List<Node> getChildrenArray() {
		return children;
	}

	public String getName() {
		return name;
	}

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
