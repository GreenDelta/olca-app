package org.openlca.app.results.analysis.sankey.model;

import java.util.ArrayList;
import java.util.List;

public abstract class Node {

	public List<Node> children = new ArrayList<>();

	public boolean addChild(Node child) {
		return children.add(child);
	}
}
