package org.openlca.app.editors.graphical.layout;

import org.openlca.app.M;


public enum GraphLayoutType {

	MINIMAL_TREE_LAYOUT(M.MinimalTree),

	TREE_LAYOUT(M.Tree);

	private String displayName;

	private GraphLayoutType(final String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

}
