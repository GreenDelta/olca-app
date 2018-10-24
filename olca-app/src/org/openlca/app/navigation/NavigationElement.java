package org.openlca.app.navigation;

import java.util.List;
import java.util.Objects;

/**
 * Basic implementation of a navigation element which manages an internal cache
 * for its child elements.
 */
abstract class NavigationElement<T> implements INavigationElement<T> {

	private List<INavigationElement<?>> cache;
	private T content;
	private INavigationElement<?> parent;

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
	public int hashCode() {
		if (content == null)
			return super.hashCode();
		return Objects.hashCode(content);
	}

}
