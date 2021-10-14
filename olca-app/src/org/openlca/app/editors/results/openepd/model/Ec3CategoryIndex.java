package org.openlca.app.editors.results.openepd.model;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

import org.openlca.util.Strings;

public class Ec3CategoryIndex {

	private final Map<String, Ec3Category> index = new HashMap<>();
	private final Map<String, String> paths = new HashMap<>();

	private Ec3CategoryIndex() {
	}

	public static Ec3CategoryIndex empty() {
		return new Ec3CategoryIndex();
	}

	public static Ec3CategoryIndex of(Ec3Category category) {
		var idx = new Ec3CategoryIndex();
		if (category == null)
			return idx;
		var queue = new ArrayDeque<Ec3Category>();
		queue.add(category);
		while (!queue.isEmpty()) {
			var next = queue.poll();
			idx.index.put(next.id, next);
			for (var sub : next.subCategories) {
				if (!sub.parents.contains(next.id)) {
					sub.parents.add(next.id);
				}
				queue.add(sub);
			}
		}
		return idx;
	}

	public String pathOf(Ec3Category category) {
		if (category == null)
			return null;
		var path = pathOf(category.id);
		return Strings.nullOrEmpty(path)
			? category.name
			: path;
	}

	public String pathOf(String id) {
		if (id == null)
			return null;
		var path = paths.get(id);
		if (path != null)
			return path;
		var cat = index.get(id);
		if (cat == null)
			return null;
		var parent = cat.parents.isEmpty()
			? null
			: pathOf(cat.parents.get(0));
		path = parent == null
			? cat.name
			: parent + "/" + cat.name;
		paths.put(id, path);
		return path;
	}
}
