package org.openlca.core.application.navigation;

import java.util.List;

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

	@Override
	public INavigationElement<?> getParent() {
		return parent;
	}

	/**
	 * Queries the child elements
	 */
	protected abstract List<INavigationElement<?>> queryChilds();

}
