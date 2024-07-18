package org.openlca.app.collaboration.browse.elements;

import java.util.List;
import java.util.Objects;

/**
 * Basic implementation of a navigation element which manages an internal cache
 * for its child elements.
 */
public abstract class ServerNavigationElement<T> implements IServerNavigationElement<T> {

	private List<IServerNavigationElement<?>> cache;
	private T content;
	private final IServerNavigationElement<?> parent;

	public ServerNavigationElement(IServerNavigationElement<?> parent, T content) {
		this.parent = parent;
		this.content = content;
	}

	@Override
	public T getContent() {
		return content;
	}

	protected void setContent(T content) {
		this.content = content;
	}

	@Override
	public IServerNavigationElement<?> getParent() {
		return parent;
	}

	@Override
	public void update() {
		cache = null;
	}

	@Override
	public List<IServerNavigationElement<?>> getChildren() {
		if (cache == null) {
			cache = queryChildren();
		}
		return cache;
	}

	protected abstract List<IServerNavigationElement<?>> queryChildren();

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

		var other = (ServerNavigationElement<?>) o;
		return Objects.equals(this.content, other.content)
				&& Objects.equals(this.parent, other.parent);
	}

}
