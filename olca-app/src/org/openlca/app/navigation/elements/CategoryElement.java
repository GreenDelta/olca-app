package org.openlca.app.navigation.elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.openlca.app.db.Database;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.Daos;
import org.openlca.core.model.Category;
import org.openlca.core.model.descriptors.Descriptor;

/**
 * Represents categories in the navigation tree.
 */
public class CategoryElement extends NavigationElement<Category> {

	public CategoryElement(INavigationElement<?> parent, Category category) {
		super(parent, category);
	}

	@Override
	public void update() {
		super.update();
		Category category = getContent();
		// refId might have changed
		setContent(new CategoryDao(Database.get()).getForId(category.id));
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
		var category = getContent();
		if (category == null)
			return Collections.emptyList();
		var lib = getLibrary().orElse(null);
		var list = new ArrayList<INavigationElement<?>>();

		// child categories
		for (var child : category.childCategories) {
			if (matches(Descriptor.of(child), lib)
					|| (lib != null && hasElementsOf(child, lib))) {
				list.add(new CategoryElement(this, child));
			}
		}

		// models in this category
		var dao = Daos.root(Database.get(), category.modelType);
		if (dao == null)
			return list;
		for (var d : dao.getDescriptors(Optional.of(category))) {
			if (matches(d, lib)) {
				list.add(new ModelElement(this, d));
			}
		}
		return list;
	}

}
