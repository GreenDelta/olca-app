package org.openlca.app.editors.processes.social;

import java.util.Objects;

import org.openlca.core.model.Category;
import org.openlca.core.model.SocialAspect;
import org.openlca.core.model.SocialIndicator;

class TreeModel {

	CategoryNode root = new CategoryNode();

	void addAspect(SocialAspect a) {
		if (a == null || a.indicator == null)
			return;
		SocialIndicator i = a.indicator;
		CategoryNode n = getNode(i.category);
		n.aspects.add(a);
	}

	CategoryNode getNode(Category c) {
		if (c == null)
			return root;
		CategoryNode parent = getNode(c.category);
		CategoryNode node = parent.findChild(c);
		if (node == null) {
			node = new CategoryNode(c);
			parent.childs.add(node);
		}
		return node;
	}

	void update(SocialAspect a) {
		if (a == null || a.indicator == null)
			return;
		CategoryNode n = getNode(a.indicator.category);
		if (n == null)
			return;
		for (SocialAspect ta : n.aspects) {
			if (Objects.equals(ta.indicator, a.indicator)) {
				Aspects.copyValues(a, ta);
				break;
			}
		}
	}

	void remove(SocialAspect a) {
		if (a == null || a.indicator == null)
			return;
		CategoryNode n = getNode(a.indicator.category);
		if (n == null)
			return;
		n.aspects.remove(a);
	}

}