package org.openlca.app.navigation.elements;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.openlca.commons.Strings;
import org.openlca.core.library.Library;
import org.openlca.core.model.descriptors.RootDescriptor;

/// Basic implementation of a navigation element which manages an internal
/// cache for its child elements.
///
/// Note that the navigation viewer uses hash lookup. Thus, the hash code of
/// an element must not change while the element is shown in the viewer.
public abstract class NavigationElement<T> implements INavigationElement<T> {

	private List<INavigationElement<?>> cache;
	private T content;
	private final INavigationElement<?> parent;
	private final String library;

	public NavigationElement(INavigationElement<?> parent, T content) {
		this.parent = parent;
		this.content = content;
		if (content instanceof Library lib) {
			library = lib.name();
		} else if (content instanceof RootDescriptor d) {
			library = Strings.nullIfBlank(d.library);
		} else {
			library = parent.getLibrary().orElse(null);
		}
	}

	@Override
	public void update() {
		cache = null;
	}

	@Override
	public List<INavigationElement<?>> getChildren() {
		if (cache == null) {
			cache = queryChilds();
		}
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

	/// Queries the child elements.
	protected abstract List<INavigationElement<?>> queryChilds();

	@Override
	public Optional<String> getLibrary() {
		return Optional.ofNullable(library);
	}

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

		// Two navigation elements are equal when their content along their path is
		// equal. The same content can be shown in different elements of the
		// navigation tree (e.g. model type elements in the database and libraries).
		// Elements without content are only equal to themselves, as their hash code
		// is the identity hash code (see above); otherwise the contract of equals
		// and hashCode would be violated, which breaks the element map of the JFace
		// viewers.
		var other = (NavigationElement<?>) o;
		if (this.content == null || other.content == null)
			return false;
		return Objects.equals(this.content, other.content)
			&& Objects.equals(this.parent, other.parent);
	}

}
