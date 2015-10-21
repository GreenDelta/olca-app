package org.openlca.app.cloud.ui;

import java.util.ArrayList;
import java.util.List;

public class DiffNode {

	private final Object content;
	private final DiffNode parent;
	private final List<DiffNode> children = new ArrayList<>();

	public DiffNode(DiffNode parent, Object content) {
		this.content = content;
		this.parent = parent;
	}

	public Object getContent() {
		return content;
	}

	public DiffNode getParent() {
		return parent;
	}

	public List<DiffNode> getChildren() {
		return children;
	}

}