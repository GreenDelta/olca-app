package org.openlca.app.util;

import org.openlca.app.db.Database;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.model.Category;

public final class Categories {

	private Categories() {
	}

	/**
	 * Removes a possible library tag from the given category. If the given
	 * category has no library tag, the unchanged category is returned. Otherwise
	 * it removes the library tag from the category and recursively of all its
	 * parent categories so that the path to this category appears in the
	 * foreground system of the navigation tree. The navigation tree should be
	 * refreshed after this call.
	 *
	 * @param category the category from which a possible library tag should be
	 *                 removed, maybe {@code null}
	 * @return the updated category if it was tagged as a library category,
	 * otherwise the unchanged category or {@code null} if the given category was
	 * {@code null}
	 */
	public static Category removeLibraryFrom(Category category) {
		if (category == null)
			return null;
		if (category.library == null)
			return category;
		var dao = new CategoryDao(Database.get());
		var c = category;
		while (c != null && c.library != null) {
			c.library = null;
			c = dao.update(c);
			c = c.category;
		}
		return dao.getForId(category.id);
	}

}
