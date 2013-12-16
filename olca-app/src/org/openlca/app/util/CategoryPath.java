package org.openlca.app.util;

import org.openlca.core.model.Category;
import org.openlca.util.Strings;

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
			parent = parent.getParentCategory();
		}
		return path;
	}

	/**
	 * Max. 2 category names and 75 characters
	 */
	public static String getShort(Category category) {
		if (category == null)
			return "";
		if (category.getParentCategory() == null)
			return category.getName();
		String shortPath = category.getParentCategory().getName() + "/"
				+ category.getName();
		return Strings.cut(shortPath, 75);
	}

}
