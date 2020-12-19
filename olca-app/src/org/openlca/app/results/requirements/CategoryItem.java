package org.openlca.app.results.requirements;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openlca.app.db.Database;
import org.openlca.core.model.Category;
import org.openlca.util.Strings;

class CategoryItem implements Item {

	final Category category;
	final List<CategoryItem> childs = new ArrayList<>();

	private CategoryItem(Category category) {
		this.category = category;
	}

	/**
	 * Builds a list of category trees over the given provider items.
	 */
	static List<CategoryItem> allOf(List<ProviderItem> providers) {
		var db = Database.get();
		if (db == null)
			return Collections.emptyList();

		// collect the set of level-0 categories
		var queue = providers.stream()
			.map(ProviderItem::categoryID)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet())
			.stream()
			.map(id -> db.get(Category.class, id))
			.filter(Objects::nonNull)
			.map(CategoryItem::new)
			.collect(Collectors.toCollection(ArrayDeque::new));

		// with the parent map, we also know when we need
		// to insert a new element into the queue
		var parents = new HashMap<Long, CategoryItem>();
		queue.forEach(item -> parents.put(item.category.id, item));

		// build the trees over the levels
		var items = new ArrayList<CategoryItem>();
		while (!queue.isEmpty()) {
			var item = queue.poll();
			var parentCategory = item.category.category;
			if (parentCategory == null) {
				// we reached a root -> add it to the
				// final list of trees
				items.add(item);
				continue;
			}

			var parent = parents.get(parentCategory.id);
			if (parent == null) {
				// found a new parent -> add it to the queue
				parent = new CategoryItem(parentCategory);
				queue.add(parent);
				parents.put(parentCategory.id, parent);
			}
			parent.childs.add(item);
		}

		sort(items);
		return items;
	}

	/**
	 * Recursively sorts the given category trees.
	 */
	private static void sort(List<CategoryItem> items) {
		items.forEach(item -> sort(item.childs));
		items.sort((i1, i2) -> Strings.compare(
			i1.category.name, i2.category.name));
	}

	@Override
	public String name() {
		return category.name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		var other = (CategoryItem) o;
		return Objects.equals(category, other.category);
	}

	@Override
	public int hashCode() {
		return Objects.hash(category);
	}
}
