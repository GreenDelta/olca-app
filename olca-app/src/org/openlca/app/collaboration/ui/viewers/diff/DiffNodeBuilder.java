package org.openlca.app.collaboration.ui.viewers.diff;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ModelType;
import org.openlca.git.model.Reference;
import org.openlca.util.Strings;

public class DiffNodeBuilder {

	private final Map<String, DiffNode> nodes = new HashMap<>();
	private final Map<String, DiffResult> results = new HashMap<>();
	private final String database;

	public DiffNodeBuilder(IDatabase database) {
		this.database = database.getName();
	}

	public DiffNode build(List<DiffResult> diffs) {
		if (!init(diffs))
			return null;
		DiffNode root = new DiffNode(null, database);
		nodes.put(null, root);
		for (DiffResult result : this.results.values()) {
			build(result);
		}
		return root;
	}

	private boolean init(List<DiffResult> diffs) {
		for (DiffResult result : diffs) {
			this.results.put(getKey(result), result);
		}
		nodes.clear();
		return this.results.size() != 0;
	}

	private void build(DiffResult result) {
		if (nodes.containsKey(getKey(result)))
			return;
		if (!result.ref().type.isCategorized())
			return;
		if (result.noAction())
			return;
		createNode(result);
	}

	private DiffNode createNode(DiffResult result) {
		Reference ref = result.ref();
		DiffNode parent = !Strings.nullOrEmpty(ref.category)
				? getOrCreateCategoryNode(ref.type, ref.category)
				: getOrCreateModelTypeNode(ref.type);
		DiffNode node = new DiffNode(parent, result);
		parent.children.add(node);
		nodes.put(getKey(result), node);
		return node;
	}

	private DiffNode getOrCreateCategoryNode(ModelType type, String category) {
		String categoryPath = type.name() + "/" + category;
		DiffNode categoryNode = nodes.get(categoryPath);
		if (categoryNode != null)
			return categoryNode;
		DiffNode parent = category.contains("/")
				? getOrCreateCategoryNode(type, category.substring(0, category.lastIndexOf("/")))
				: getOrCreateModelTypeNode(type);
		categoryNode = new DiffNode(parent, categoryPath);
		parent.children.add(categoryNode);
		nodes.put(categoryPath, categoryNode);
		return categoryNode;

	}

	private DiffNode getOrCreateModelTypeNode(ModelType type) {
		DiffNode typeNode = nodes.get(type.name());
		if (typeNode != null)
			return typeNode;
		DiffNode root = nodes.get(null);
		typeNode = new DiffNode(root, type);
		root.children.add(typeNode);
		nodes.put(type.name(), typeNode);
		return typeNode;
	}

	private String getKey(DiffResult result) {
		var ref = result.ref();
		return ref.type.name() + ref.refId;
	}

}
