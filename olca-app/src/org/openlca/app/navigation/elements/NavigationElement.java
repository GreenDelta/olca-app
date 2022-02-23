package org.openlca.app.navigation.elements;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.openlca.app.db.Database;
import org.openlca.core.database.Daos;
import org.openlca.core.library.Library;
import org.openlca.core.model.Category;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.util.Strings;

/**
 * Basic implementation of a navigation element which manages an internal cache
 * for its child elements.
 */
abstract class NavigationElement<T> implements INavigationElement<T> {

	private List<INavigationElement<?>> cache;
	private T content;
	private final INavigationElement<?> parent;

	public NavigationElement(INavigationElement<?> parent, T content) {
		this.parent = parent;
		this.content = content;
	}

	@Override
	public void update() {
		cache = null;
	}

	@Override
	public List<INavigationElement<?>> getChildren() {
		if (cache == null)
			cache = queryChilds();
		return cache;
	}

	@Override
	public T getContent() {
		return content;
	}

	protected void setContent(T content) {
		this.content = content;
	}

	@Override
	public INavigationElement<?> getParent() {
		return parent;
	}

	/**
	 * Queries the child elements
	 */
	protected abstract List<INavigationElement<?>> queryChilds();

	@Override
	public final int hashCode() {
		// include the contents and parents recursively along
		// the path to calculate the hash
		if (content == null)
			return super.hashCode();
		var parent = getParent();
		return parent == null
			? content.hashCode()
			: Objects.hash(content, getParent());
	}

	@Override
	public final boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		// two navigation elements are equal when their content
		// along their path are equal because the same content
		// can be shown in different elements of the navigation
		// tree (e.g. model type elements in the database and
		// libraries)

		var other = (NavigationElement<?>) o;
		return Objects.equals(this.content, other.content)
			&& Objects.equals(this.parent, other.parent);
	}

	static boolean matches(Descriptor d, Library lib) {
		if (d == null)
			return false;
		if (lib == null || lib.getInfo() == null)
			return !d.isFromLibrary();
		if (Strings.nullOrEmpty(d.library))
			return false;
		var libID = lib.getInfo().id();
		return Strings.nullOrEqual(libID, d.library);
	}

	/**
	 * Returns true if the given category contains (recursively) elements
	 * from the given library.
	 */
	static boolean hasElementsOf(Category category, String library) {
		if (category == null || library == null)
			return false;
		if (Strings.nullOrEqual(category.library, library))
			return true;
		for (var child : category.childCategories) {
			if (hasElementsOf(child, library))
				return true;
		}
		var db = Database.get();
		if (db == null || category.modelType == null)
			return false;
		var dao = Daos.root(db, category.modelType);
		if (dao == null)
			return false;
		return dao.getDescriptors(Optional.of(category))
			.stream()
			.anyMatch(d -> Strings.nullOrEqual(d.library, library));
	}
}
