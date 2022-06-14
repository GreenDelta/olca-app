package org.openlca.app.editors.graphical_legacy.layout;

import org.openlca.app.M;


public enum LayoutType {

	MINIMAL_TREE_LAYOUT(M.MinimalTree),

	TREE_LAYOUT(M.Tree);

	private final String displayName;

	LayoutType(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

}
