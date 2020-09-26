package org.openlca.app.results.analysis.sankey.model;

import java.util.ArrayList;
import java.util.List;

public abstract class Node implements Comparable<Node> {

	public List<Node> children = new ArrayList<>();
	Node parent;

	public boolean addChild(Node child) {
		boolean b = children.add(child);
		if (b) {
			child.parent = this;
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
