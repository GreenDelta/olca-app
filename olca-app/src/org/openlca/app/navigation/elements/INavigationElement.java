package org.openlca.app.navigation.elements;

import java.util.List;
import java.util.Optional;

import org.openlca.core.database.DataPackage;

/**
 * Interface for elements in the navigation tree.
 */
public interface INavigationElement<T> {

	INavigationElement<?> getParent();

	List<INavigationElement<?>> getChildren();

	default boolean hasChildren() {
		return !getChildren().isEmpty();
	}

	T getContent();

	void update();

	/**
	 * Returns an optional data package filter that is set on this element. For the
	 * navigation this means, that only the content of this data package should be
	 * shown and that specific actions are not allowed here.
	 */
	default Optional<DataPackage> getDataPackage() {
		return Optional.empty();
	}
}
