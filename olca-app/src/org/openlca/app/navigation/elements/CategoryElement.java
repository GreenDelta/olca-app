package org.openlca.app.navigation.elements;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.persistence.Table;
import org.openlca.app.db.Database;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.Daos;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.NativeSql;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;

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
		var list = new ArrayList<INavigationElement<?>>();

		// child categories
		for (var child : category.childCategories) {
			list.add(new CategoryElement(this, child));
		}

		// models in this category
		var dao = Daos.root(Database.get(), category.modelType);
		if (dao == null)
			return list;
		for (var d : dao.getDescriptors(Optional.of(category))) {
			list.add(new ModelElement(this, d));
		}
		return list;
	}

	/**
	 * Returns {@code true} if the category of this element or a child category
	 * of it contain model elements from a library.
	 */
	public boolean hasLibraryContent() {
		return hasLibraryContent(getContent());
	}

	/**
	 * Returns {@code true} if the category of this element or a child category
	 * of it contain model elements from the given library.
	 */
	public boolean hasLibraryContent(String library) {
		return hasLibraryContent(getContent(), library);
	}

	/**
	 * Returns {@code true} if the category of this element or a child category
	 * of it contain model elements that do not belong to a library.
	 */
	public boolean hasNonLibraryContent() {
		return hasNonLibraryContent(getContent());
	}

	/**
	 * Returns {@code true} if the given category or a child category of it
	 * contain model elements from a library.
	 */
	public static boolean hasLibraryContent(Category category) {
		if (category == null)
			return false;
		var test = ContentTest.forAnyLibrary(Database.get(), category.modelType);
		return test != null && test.test(category);
	}

	/**
	 * Returns {@code true} if the given category or a child category of it
	 * contain model elements from the given library.
	 */
	public static boolean hasLibraryContent(Category category, String library) {
		if (category == null)
			return false;
		var test = ContentTest.forLibrary(
			Database.get(), category.modelType, library);
		return test != null && test.test(category);
	}

	/**
	 * Returns {@code true} if the given category or a child category of it
	 * contain model elements that do not belong to a library.
	 */
	public static boolean hasNonLibraryContent(Category category) {
		if (category == null)
			return false;
		var test = ContentTest.forNonLibrary(Database.get(), category.modelType);
		return test != null && test.test(category);
	}

	private record ContentTest(NativeSql sql, String prefix, String suffix) {

		public static ContentTest forAnyLibrary(IDatabase db, ModelType type) {
			return of(db, type, " and library is not null");
		}

		public static ContentTest forLibrary(
			IDatabase db, ModelType type, String library) {
			return of(db, type, " and library='" + library + "'");
		}

		public static ContentTest forNonLibrary(IDatabase db, ModelType type) {
			return of(db, type, " and library is null");
		}

		private static ContentTest of(IDatabase db, ModelType type, String suffix) {
			var prefix = prefixOf(type);
			if (prefix == null || db == null)
				return null;
			return new ContentTest(NativeSql.on(db), prefix, suffix);
		}

		private static String prefixOf(ModelType type) {
			if (type == null || !type.isRoot())
				return null;
			var modelClass = type.getModelClass();
			if (modelClass == null)
				return null;
			var table = modelClass.getAnnotation(Table.class);
			if (table == null)
				return null;
			return "select count(id) from " + table.name() + " where f_category = ";
		}

		private boolean testOne(Category category) {
			if (category == null)
				return false;
			var b = new AtomicBoolean(false);
			sql.query(prefix + category.id + suffix, r -> {
				var count = r.getInt(1);
				b.set(count > 0);
				return false;
			});
			return b.get();
		}

		private boolean test(Category category) {
			var q = new ArrayDeque<Category>();
			q.add(category);
			while (!q.isEmpty()) {
				var next = q.poll();
				if (testOne(next))
					return true;
				q.addAll(next.childCategories);
			}
			return false;
		}
	}

}
