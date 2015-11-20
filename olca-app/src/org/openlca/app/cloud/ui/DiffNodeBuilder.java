package org.openlca.app.cloud.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.cloud.index.DiffIndex;
import org.openlca.app.cloud.ui.DiffResult.DiffResponse;
import org.openlca.cloud.model.data.DatasetDescriptor;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;

public class DiffNodeBuilder {

	private final Map<String, DiffNode> nodes = new HashMap<>();
	private final Map<String, DiffResult> diffs = new HashMap<>();
	private final CategoryDao categoryDao;
	private final DiffIndex index;
	private final String database;

	public DiffNodeBuilder(IDatabase database, DiffIndex index) {
		this.categoryDao = new CategoryDao(database);
		this.index = index;
		this.database = database.getName();
	}

	public DiffNode build(List<DiffResult> diffs) {
		for (DiffResult result : diffs)
			if (result.getType() != DiffResponse.NONE)
				this.diffs.put(result.getDescriptor().getRefId(), result);
		if (this.diffs.size() == 0)
			return null;
		nodes.clear();
		DiffNode root = new DiffNode(null, database);
		nodes.put("", root);
		for (DiffResult result : this.diffs.values()) {
			if (nodes.containsKey(result.getDescriptor().getRefId()))
				continue;
			if (!result.getDescriptor().getType().isCategorized())
				continue;
			DiffNode parent = getOrCreateParentNode(result);
			DiffNode node = new DiffNode(parent, result);
			parent.children.add(node);
			nodes.put(result.getDescriptor().getRefId(), node);
		}
		return root;
	}

	private DiffNode getOrCreateParentNode(DiffResult result) {
		if (result.remote != null)
			return getOrCreateParentNode(result.remote);
		return getOrCreateParentNode(result.local.getDescriptor());
	}

	private DiffNode getOrCreateParentNode(DatasetDescriptor descriptor) {
		String parentId = descriptor.getCategoryRefId();
		ModelType categoryType = descriptor.getType();
		if (categoryType == ModelType.CATEGORY)
			categoryType = descriptor.getCategoryType();
		if (parentId == null)
			return getOrCreateModelTypeNode(categoryType);
		DiffNode categoryNode = nodes.get(parentId);
		if (categoryNode != null)
			return categoryNode;
		DiffResult result = diffs.get(parentId);
		if (result != null)
			return createNodeFromDiff(result);
		Category category = categoryDao.getForRefId(parentId);
		return createNodeFromCategory(category);
	}

	private DiffNode createNodeFromCategory(Category category) {
		DiffNode parent = getOrCreateParentNode(CloudUtil
				.toDescriptor(category));
		DiffResult result = new DiffResult(index.get(category.getRefId()));
		DiffNode node = new DiffNode(parent, result);
		parent.children.add(node);
		nodes.put(category.getRefId(), node);
		return node;
	}

	private DiffNode createNodeFromDiff(DiffResult result) {
		DiffNode parent = getOrCreateParentNode(result.getDescriptor());
		DiffNode node = new DiffNode(parent, result);
		parent.children.add(node);
		nodes.put(result.getDescriptor().getRefId(), node);
		return node;
	}

	private DiffNode getOrCreateModelTypeNode(ModelType type) {
		DiffNode typeNode = nodes.get(type.name());
		if (typeNode != null)
			return typeNode;
		DiffNode root = nodes.get("");
		typeNode = new DiffNode(root, type);
		root.children.add(typeNode);
		nodes.put(type.name(), typeNode);
		return typeNode;
	}

}
