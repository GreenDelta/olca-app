package org.openlca.app.collaboration.navigation;

import java.util.Stack;
import java.util.function.Predicate;

import org.openlca.app.collaboration.navigation.NavElement.ElementType;
import org.openlca.app.navigation.elements.CategoryElement;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.GroupElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.LibraryDirElement;
import org.openlca.app.navigation.elements.LibraryElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.navigation.elements.ModelTypeElement;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.git.repo.ClientRepository;

class NavFinder {

	private final ClientRepository repo;

	NavFinder(ClientRepository repo) {
		this.repo = repo;
	}

	NavElement find(NavElement root, INavigationElement<?> elem) {
		if (elem instanceof DatabaseElement)
			return root;
		if (elem instanceof LibraryDirElement)
			return findLibraryDir(root);
		if (elem instanceof LibraryElement e)
			return findLibrary(root, e.getContent().name());
		if (elem instanceof GroupElement e)
			return findGroup(root, e.getContent().label);
		if (elem instanceof ModelTypeElement e)
			return findModelType(root, e.getContent());
		if (elem instanceof CategoryElement e)
			return findCategory(root, e.getContent());
		if (elem instanceof ModelElement e)
			return findDescriptor(root, e.getContent());
		return null;
	}

	private NavElement findLibraryDir(NavElement root) {
		return findChild(root, elem -> elem.is(ElementType.LIBRARY_DIR));
	}

	private NavElement findLibrary(NavElement root, String id) {
		var libraryDir = findLibraryDir(root);
		if (libraryDir == null)
			return null;
		return findChild(libraryDir, elem -> elem.is(ElementType.LIBRARY) && elem.content().equals(id));
	}

	private NavElement findGroup(NavElement root, String label) {
		return findChild(root, elem -> elem.is(ElementType.GROUP) && elem.content().equals(label));
	}

	private NavElement findModelType(NavElement root, ModelType type) {
		for (var child : root.children()) {
			if (child.is(ElementType.MODEL_TYPE) && child.content().equals(type))
				return child;
			if (child.is(ElementType.GROUP)) {
				var typeElement = findModelType(child, type);
				if (typeElement != null)
					return typeElement;
			}
		}
		return null;
	}

	private NavElement findCategory(NavElement root, Category category) {
		var typeElement = findModelType(root, category.modelType);
		var stack = new Stack<Category>();
		var current = category;
		while (current != null) {
			stack.add(current);
			current = current.category;
		}
		var element = typeElement;
		while (!stack.isEmpty()) {
			var next = stack.pop();
			element = findChild(element, elem -> elem.is(ElementType.CATEGORY) && elem.content().equals(next));
		}
		return element;
	}

	private NavElement findDescriptor(NavElement root, RootDescriptor descriptor) {
		var category = repo.descriptors.getCategory(descriptor.category);
		var parentElement = category != null
				? findCategory(root, category)
				: findModelType(root, descriptor.type);
		return findChild(parentElement, elem -> elem.is(ElementType.MODEL) && elem.content().equals(descriptor));
	}

	private static NavElement findChild(NavElement parent, Predicate<NavElement> check) {
		if (parent == null)
			return null;
		return parent.children().stream().filter(check).findFirst().orElse(null);
	}

}
