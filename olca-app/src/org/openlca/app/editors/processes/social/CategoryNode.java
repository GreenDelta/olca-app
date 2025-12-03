package org.openlca.app.editors.processes.social;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.openlca.core.model.Category;
import org.openlca.core.model.SocialAspect;

class CategoryNode {

	final Category category;
	final List<CategoryNode> childs = new ArrayList<>();
	final List<SocialAspect> aspects = new ArrayList<>();

	CategoryNode() {
		category = null;
	}

	CategoryNode(Category c) {
		category = c;
	}

	String name() {
		return category != null
			? category.name
			: null;
	}

	/// A category node is empty when it is a tree without social aspects.
	boolean isEmpty() {
		if (!aspects.isEmpty())
			return false;
		for (var c : childs) {
			if (!c.isEmpty())
				return false;
		}
		return true;
	}

	CategoryNode findChild(Category c) {
		for (CategoryNode child : childs) {
			if (Objects.equals(c, child.category))
				return child;
		}
		return null;
	}

	List<SocialAspect> getAllAscpectsRecursively() {
		var all = new ArrayList<>(aspects);
		for (var c : childs) {
			all.addAll(c.getAllAscpectsRecursively());
		}
		return all;
	}
}