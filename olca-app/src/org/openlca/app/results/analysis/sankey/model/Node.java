package org.openlca.app.results.analysis.sankey.model;

import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

abstract class Node implements Comparable<Node> {

	public static String PROPERTY_ADD = "NodeAddChild";
	public static String PROPERTY_LAYOUT = "Layout";
	public static String PROPERTY_REMOVE = "NodeRemoveChild";

	public List<Node> children = new ArrayList<>();
	Node parent;
	PropertyChangeSupport listeners = new PropertyChangeSupport(this);

	public boolean addChild(Node child) {
		boolean b = children.add(child);
		if (b) {
			child.parent = this;
			listeners.firePropertyChange(PROPERTY_ADD, null, child);
		}
		return b;
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

	public String getName() {
		return "Unknown";
	}

}
