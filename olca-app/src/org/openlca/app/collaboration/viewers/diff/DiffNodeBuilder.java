package org.openlca.app.collaboration.viewers.diff;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.openlca.commons.Strings;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ModelType;
import org.openlca.git.RepositoryInfo;
import org.openlca.git.model.TriDiff;

public class DiffNodeBuilder {

	private final Map<String, DiffNode> nodes = new LinkedHashMap<>();
	private final Map<String, TriDiff> diffs = new LinkedHashMap<>();
	private final IDatabase database;

	public DiffNodeBuilder(IDatabase database) {
		this.database = database;
	}

	public DiffNode build(Collection<TriDiff> diffs) {
		if (!init(diffs))
			return null;
		var root = new DiffNode(null, database.getName());
		nodes.put(null, root);
		for (var diff : this.diffs.values()) {
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
		if (diff.noAction())
			return;
		createNode(diff);
	}

	private void createNode(TriDiff diff) {
		var parent = Strings.isNotBlank(diff.category)
				? getOrCreateCategoryNode(diff.type, diff.category)
				: diff.isLibrary
						? getOrCreateLibrariesNode()
						: getOrCreateModelTypeNode(diff.type);
		var node = new DiffNode(parent, diff);
		parent.children.add(node);
		nodes.put(getKey(diff), node);
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

	private DiffNode getOrCreateLibrariesNode() {
		var librariesNode = nodes.get(RepositoryInfo.FILE_NAME);
		if (librariesNode != null)
			return librariesNode;
		var root = nodes.get(null);
		librariesNode = new DiffNode(root, null);
		root.children.add(librariesNode);
		nodes.put(RepositoryInfo.FILE_NAME, librariesNode);
		return librariesNode;
	}

	private String getKey(TriDiff diff) {
		return diff.path;
	}

}
