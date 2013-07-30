package org.openlca.app;

import org.openlca.core.model.Category;

public class CategoryPath {

	private CategoryPath() {
	}

	/**
	 * Returns the full category path from the root category to this category,
	 * or an empty string if the given category is null.
	 */
	public static String getFull(Category category) {
		if (category == null)
			return "";
		String path = category.getName();
		Category parent = category.getParentCategory();
		while (parent != null) {
			path = parent.getName() + "/" + path;
		}
		return path;
	}

	/**
	 * Max. 2 category names
	 */
	public static String getShort(Category category) {
		if (category == null)
			return "";
		if (category.getParentCategory() == null)
			return category.getName();
		return category.getParentCategory().getName() + "/"
				+ category.getName();
	}

}
