package org.openlca.app.editors.processes.social;

import org.openlca.core.model.Category;
import org.openlca.core.model.SocialIndicator;

class TreeModel {

	CategoryNode root = new CategoryNode();

	void addAspect(SocialAspect a) {
		if (a == null || a.indicator == null)
			return;
		SocialIndicator i = a.indicator;
		CategoryNode n = getNode(i.getCategory());
		n.aspects.add(a);
	}

	CategoryNode getNode(Category c) {
		if (c == null)
			return root;
		CategoryNode parent = getNode(c.getParentCategory());
		CategoryNode node = parent.findChild(c);
		if (node == null) {
			node = new CategoryNode(c);
			parent.childs.add(node);
		}
		return node;
	}

}