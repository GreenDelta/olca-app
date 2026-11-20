package org.openlca.app.navigation.elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.openlca.app.db.Database;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.Daos;
import org.openlca.core.model.Category;

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
		var category = getContent();
		var db = Database.get();
		if (category == null || db == null)
			return;
		// refId might have changed; if the category does not exist anymore, we
		// keep the old content, as setting it to null would change the hash code
		// of this element and of all its child elements while they are shown in
		// the navigation tree
		var newContent = new CategoryDao(db).getForId(category.id);
		if (newContent != null) {
			setContent(newContent);
		}
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
		var category = getContent();
		if (category == null)
			return Collections.emptyList();

		var lib = getLibrary().orElse(null);
		var list = new ArrayList<INavigationElement<?>>();

		// child categories
		if (lib == null) {
			category.childCategories.stream()
				.map(c -> new CategoryElement(this, c))
				.forEach(list::add);
		} else {
			var test = DatabaseElement.categoryTesterOf(this);
			if (test != null) {
				category.childCategories.stream()
					.filter(c -> test.hasLibraryContent(c, lib))
					.map(c -> new CategoryElement(this, c))
					.forEach(list::add);
			}
		}

		// models in this category
		var dao = Daos.root(Database.get(), category.modelType);
		if (dao == null)
			return list;
		for (var d : dao.getDescriptors(Optional.of(category))) {
			if (lib == null || lib.equals(d.library)) {
				list.add(new ModelElement(this, d));
			}
		}
		return list;
	}

	/**
	 * Returns {@code true} if the category of this element or a child category
	 * of it contain model elements from a library.
	 */
	public boolean hasLibraryContent() {
		var test = DatabaseElement.categoryTesterOf(this);
		return test != null && test.hasLibraryContent(getContent());
	}

	/**
	 * Returns {@code true} if the category of this element or a child category
	 * of it contain model elements from the given library.
	 */
	public boolean hasLibraryContent(String library) {
		var test = DatabaseElement.categoryTesterOf(this);
		return test != null && test.hasLibraryContent(getContent(), library);
	}

	/**
	 * Returns {@code true} if the category of this element or a child category
	 * of it contain model elements only from the given library.
	 */
	public boolean hasOnlyLibraryContent(String library) {
		var test = DatabaseElement.categoryTesterOf(this);
		return test != null && test.hasOnlyLibraryContent(getContent(), library);
	}

	/// Returns `true` when the category of this element or any child category
	/// of it contain model elements that do not belong to a library.
	public boolean hasNonLibraryContent() {
		var test = DatabaseElement.categoryTesterOf(this);
		if (test != null)
			return test.hasNonLibraryContent(getContent());
		// there is no content test when the database has no libraries; then all
		// models are non-library models, and we just check if there is a model in
		// this category or one of its sub-categories
		for (var child : getChildren()) {
			if (child instanceof ModelElement)
				return true;
			if (child instanceof CategoryElement c && c.hasNonLibraryContent())
				return true;
		}
		return false;
	}

}
