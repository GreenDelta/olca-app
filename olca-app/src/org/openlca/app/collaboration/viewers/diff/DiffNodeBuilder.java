package org.openlca.app.collaboration.viewers.diff;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ModelType;
import org.openlca.util.Strings;

public class DiffNodeBuilder {

	private final Map<String, DiffNode> nodes = new HashMap<>();
	private final Map<String, TriDiff> diffs = new HashMap<>();
	private final String database;

	public DiffNodeBuilder(IDatabase database) {
		this.database = database.getName();
	}

	public DiffNode build(Collection<TriDiff> diffs) {
		if (!init(diffs))
			return null;
		var root = new DiffNode(null, database);
		nodes.put(null, root);
		for (TriDiff diff : this.diffs.values()) {
			build(diff);
		}
		return root;
	}

	private boolean init(Collection<TriDiff> diffs) {
		for (var result : diffs) {
			this.diffs.put(getKey(result), result);
		}
		nodes.clear();
		return this.diffs.size() != 0;
	}

	private void build(TriDiff diff) {
		if (nodes.containsKey(getKey(diff)))
			return;
		if (!diff.type.isRoot())
			return;
		if (diff.noAction())
			return;
		createNode(diff);
	}

	private DiffNode createNode(TriDiff diff) {
		var parent = !Strings.nullOrEmpty(diff.category)
				? getOrCreateCategoryNode(diff.type, diff.category)
				: getOrCreateModelTypeNode(diff.type);
		var node = new DiffNode(parent, diff);
		parent.children.add(node);
		nodes.put(getKey(diff), node);
		return node;
	}

	private DiffNode getOrCreateCategoryNode(ModelType type, String category) {
		var categoryPath = type.name() + "/" + category;
		var categoryNode = nodes.get(categoryPath);
		if (categoryNode != null)
			return categoryNode;
		var parent = category.contains("/")
				? getOrCreateCategoryNode(type, category.substring(0, category.lastIndexOf("/")))
				: getOrCreateModelTypeNode(type);
		categoryNode = new DiffNode(parent, categoryPath);
		parent.children.add(categoryNode);
		nodes.put(categoryPath, categoryNode);
		return categoryNode;

	}

	private DiffNode getOrCreateModelTypeNode(ModelType type) {
		var typeNode = nodes.get(type.name());
		if (typeNode != null)
			return typeNode;
		var root = nodes.get(null);
		typeNode = new DiffNode(root, type);
		root.children.add(typeNode);
		nodes.put(type.name(), typeNode);
		return typeNode;
	}

	private String getKey(TriDiff diff) {
		return diff.path;
	}

}
