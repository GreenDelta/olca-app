package org.openlca.app.navigation.elements;

import java.util.List;
import java.util.Optional;

import org.openlca.core.library.Library;

/**
 * Interface for elements in the navigation tree.
 */
public interface INavigationElement<T> {

	INavigationElement<?> getParent();

	List<INavigationElement<?>> getChildren();

	T getContent();

	void update();

	/**
	 * Returns the corresponding library if this element is under a library
	 * element.
	 */
	default Optional<Library> getLibrary() {
		if (this instanceof LibraryElement)
			return Optional.of(((LibraryElement) this).getContent());
		var parent = getParent();
		return parent == null
				? Optional.empty()
				: parent.getLibrary();
	}

}
