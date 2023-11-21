package org.openlca.app.navigation.actions.libraries;

import java.util.ArrayList;
import java.util.List;

import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.elements.CategoryElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.Daos;
import org.openlca.core.library.Library;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;

class LibUtils {

	static void unmountUnsafe(Library lib, INavigationElement<?> root) {
		var libraryCategories = collectCategories(lib, root);
		for (var type : ModelType.values()) {
			if (type == ModelType.CATEGORY)
				continue;
			var dao = Daos.root(Database.get(), type);
			for (var descriptor : dao.getDescriptors()) {
				if (!descriptor.isFromLibrary() || !lib.name().equals(descriptor.library))
					continue;
				dao.delete(descriptor.id);
			}
		}
		var categoryDao = new CategoryDao(Database.get());
		for (var category : libraryCategories) {
			categoryDao.delete(category);
		}
		Database.get().removeLibrary(lib.name());
		Navigator.refresh();
	}

	static List<Category> collectCategories(Library lib, INavigationElement<?> element) {
		var categories = new ArrayList<Category>();
		for (var child : element.getChildren()) {
			categories.addAll(collectCategories(lib, child));
		}
		if (element instanceof CategoryElement c) {
			Category category = c.getContent();
			if (c.hasOnlyLibraryContent(lib.name()) && categories.size() == c.getContent().childCategories.size()) {
				categories.add(category);
			}
		}
		return categories;
	}

}
