package org.openlca.app.navigation.elements;

import java.util.List;
import java.util.Optional;

/**
 * Interface for elements in the navigation tree.
 */
public interface INavigationElement<T> {

	INavigationElement<?> getParent();

	List<INavigationElement<?>> getChildren();

	T getContent();

	void update();

	/**
	 * Returns an optional library filter that is set on this element. For the
	 * navigation this means, that only the content of this library should be
	 * shown and that specific actions are not allowed here.
	 */
	default Optional<String> getLibrary() {
		return Optional.empty();
	}
}
