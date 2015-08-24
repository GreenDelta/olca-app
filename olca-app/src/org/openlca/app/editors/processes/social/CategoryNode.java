package org.openlca.app.editors.processes.social;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.openlca.core.model.Category;
import org.openlca.core.model.SocialAspect;

class CategoryNode {

	Category category;
	List<CategoryNode> childs = new ArrayList<>();
	List<SocialAspect> aspects = new ArrayList<>();

	CategoryNode() {
	}

	CategoryNode(Category c) {
		category = c;
	}

	CategoryNode findChild(Category c) {
		for (CategoryNode child : childs) {
			if (Objects.equals(c, child.category))
				return child;
		}
		return null;
	}
}