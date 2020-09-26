package org.openlca.app.results.analysis.sankey.model;

import java.util.ArrayList;
import java.util.List;

public abstract class Node {

	public List<Node> children = new ArrayList<>();
	Node parent;

	public boolean addChild(Node child) {
		boolean b = children.add(child);
		if (b) {
			child.parent = this;
		}
		return b;
	}
}
