package org.openlca.app.navigation.elements;

import java.util.List;

/**
 * Interface for elements in the navigation tree.
 */
public interface INavigationElement<T> {

	INavigationElement<?> getParent();

	List<INavigationElement<?>> getChildren();

	T getContent();

	void update();

}
