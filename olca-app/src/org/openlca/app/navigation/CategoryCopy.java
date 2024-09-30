package org.openlca.app.navigation;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.openlca.app.db.Database;
import org.openlca.app.navigation.elements.CategoryElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.util.Categories;
import org.openlca.util.Strings;


class CategoryCopy {

	private final CategoryElement source;
	private final INavigationElement<?> target;
	private BiConsumer<ModelElement, Category> modelFn;

	private final ModelType type;
	private final IDatabase db;

	private CategoryCopy(
			CategoryElement source,
			INavigationElement<?> target,
			BiConsumer<ModelElement, Category> modelFn
	) {
		this.source = source;
		this.target = target;
		this.modelFn = modelFn;
		this.type = source.getContent().modelType;
		this.db = Database.get();
	}

	static void create(
			CategoryElement source,
			INavigationElement<?> target,
			BiConsumer<ModelElement, Category> modelFn
	) {
		if (source == null
				|| target == null
				|| modelFn == null
				|| Database.get() == null
				|| source.getContent() == null
				|| source.getContent().modelType == null)
			return;
		new CategoryCopy(source, target, modelFn).doIt();
	}

	private void doIt() {
		var queue = new ArrayDeque<Node>();
		queue.add(new Node(source, targetRootPath()));
		var dao = new CategoryDao(db);
		while (!queue.isEmpty()) {
			var node = queue.poll();
			var category = dao.sync(type, node.path);
			for (var child : node.elem.getChildren()) {
				if (child instanceof CategoryElement ce) {
					queue.add(node.next(ce));
				} else if (child instanceof ModelElement me) {
					modelFn.accept(me, category);
				}
			}
		}
	}

	private String[] targetRootPath() {

		var targetRoot = target instanceof CategoryElement e
				? e.getContent()
				: null;
		var targetChilds = targetRoot == null
				? new CategoryDao(db).getRootCategories(type)
				: targetRoot.childCategories;
		var existingChilds = targetChilds.stream()
				.map(c -> c.name)
				.filter(Objects::nonNull)
				.map(s -> s.strip().toLowerCase(Locale.US))
				.collect(Collectors.toSet());

		// the name of the new root in the copy target
		// should be unique; we append a `copy` prefix
		// if this is not the case
		var base = Strings.orEmpty(source.getContent().name).strip();
		var name = base;
		int count = 0;
		while (existingChilds.contains(name.toLowerCase(Locale.US))) {
			var suffix = count > 0
					? " (copy " + count + ")"
					: " (copy)";
			name = base + suffix;
			count++;
		}

		List<String> basePath = targetRoot != null
				? Categories.path(targetRoot)
				: List.of();
		var path = new String[basePath.size() + 1];
		for (int i = 0; i < basePath.size(); i++) {
			path[i] = basePath.get(i);
		}
		path[basePath.size()] = name;
		return path;
	}

	private record Node(CategoryElement elem, String[] path) {

		Node next(CategoryElement child) {
			if (child.getContent() == null)
				return this;
			var nextPath = Arrays.copyOf(path, path.length + 1);
			nextPath[path.length] = child.getContent().name;
			return new Node(child, nextPath);
		}
	}
}
