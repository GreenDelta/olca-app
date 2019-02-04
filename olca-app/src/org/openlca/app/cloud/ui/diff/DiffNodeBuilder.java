package org.openlca.app.cloud.ui.diff;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openlca.app.cloud.index.DiffIndex;
import org.openlca.app.cloud.ui.diff.DiffResult.DiffResponse;
import org.openlca.cloud.model.data.Dataset;
import org.openlca.cloud.util.Datasets;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;

public class DiffNodeBuilder {

	private final Map<String, DiffNode> nodes = new HashMap<>();
	private final Map<String, DiffResult> diffs = new HashMap<>();
	private final Map<String, Category> categories = new HashMap<>();
	private final DiffIndex index;
	private final String database;

	public DiffNodeBuilder(IDatabase database, DiffIndex index) {
		putCategories(new CategoryDao(database).getRootCategories());
		this.index = index;
		this.database = database.getName();
	}

	private void putCategories(List<Category> categories) {
		for (Category c : categories) {
			this.categories.put(c.refId, c);
			putCategories(c.childCategories);
		}
	}

	public DiffNode build(List<DiffResult> diffs) {
		if (!init(diffs))
			return null;
		DiffNode root = new DiffNode(null, database);
		nodes.put("", root);
		for (DiffResult result : this.diffs.values())
			build(result);
		return root;
	}

	private boolean init(List<DiffResult> diffs) {
		for (DiffResult result : diffs)
			this.diffs.put(result.getDataset().refId, result);
		nodes.clear();
		return this.diffs.size() != 0;
	}

	private void build(DiffResult result) {
		if (nodes.containsKey(result.getDataset().refId))
			return;
		if (!result.getDataset().type.isCategorized())
			return;
		if (result.getType() == DiffResponse.NONE)
			return;
		DiffNode parent = getOrCreateParentNode(result.getDataset());
		DiffNode node = new DiffNode(parent, result);
		parent.children.add(node);
		nodes.put(result.getDataset().refId, node);
	}

	private DiffNode getOrCreateParentNode(Dataset dataset) {
		String parentId = dataset.categoryRefId;
		ModelType categoryType = dataset.type;
		if (categoryType == ModelType.CATEGORY)
			categoryType = dataset.categoryType;
		if (parentId == null)
			return getOrCreateModelTypeNode(categoryType);
		DiffNode categoryNode = nodes.get(parentId);
		if (categoryNode != null)
			return categoryNode;
		DiffResult result = diffs.get(parentId);
		if (result != null)
			return createNodeFromDiff(result);
		Category category = categories.get(parentId);
		return createNodeFromCategory(category);
	}

	private DiffNode createNodeFromCategory(Category category) {
		DiffNode parent = getOrCreateParentNode(Datasets.toDataset(category));
		DiffResult result = new DiffResult(index.get(category.refId));
		DiffNode node = new DiffNode(parent, result);
		parent.children.add(node);
		nodes.put(category.refId, node);
		return node;
	}

	private DiffNode createNodeFromDiff(DiffResult result) {
		DiffNode parent = getOrCreateParentNode(result.getDataset());
		DiffNode node = new DiffNode(parent, result);
		parent.children.add(node);
		nodes.put(result.getDataset().refId, node);
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
