package org.openlca.app.cloud.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.cloud.index.DiffIndex;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;

import com.greendelta.cloud.model.data.DatasetDescriptor;

public class DiffNodeBuilder {

	private final Map<String, Node> nodes = new HashMap<>();
	private final Map<String, DiffResult> diffs = new HashMap<>();
	private final CategoryDao categoryDao;
	private final DiffIndex index;
	private final String database;

	public DiffNodeBuilder(IDatabase database, DiffIndex index) {
		this.categoryDao = new CategoryDao(database);
		this.index = index;
		this.database = database.getName();
	}

	public Node build(List<DiffResult> diffs) {
		for (DiffResult result : diffs)
			this.diffs.put(result.getDescriptor().getRefId(), result);
		nodes.clear();
		Node root = new Node(null, database);
		nodes.put("", root);
		for (DiffResult result : diffs) {
			if (nodes.containsKey(result.getDescriptor().getRefId()))
				continue;
			Node parent = getOrCreateParentNode(result);
			Node node = new Node(parent, result);
			parent.getChildren().add(node);
			nodes.put(result.getDescriptor().getRefId(), node);
		}
		return root;
	}

	private Node getOrCreateParentNode(DiffResult result) {
		if (result.remote != null)
			return getOrCreateParentNode(result.remote);
		return getOrCreateParentNode(result.local.getDescriptor());
	}

	private Node getOrCreateParentNode(DatasetDescriptor descriptor) {
		String parentId = descriptor.getCategoryRefId();
		ModelType categoryType = descriptor.getType();
		if (categoryType == ModelType.CATEGORY)
			categoryType = descriptor.getCategoryType();
		if (parentId == null)
			return getOrCreateModelTypeNode(categoryType);
		Node categoryNode = nodes.get(parentId);
		if (categoryNode != null)
			return categoryNode;
		DiffResult result = diffs.get(parentId);
		if (result != null)
			return createNodeFromDiff(result);
		Category category = categoryDao.getForRefId(parentId);
		return createNodeFromCategory(category);
	}

	private Node createNodeFromCategory(Category category) {
		Node parent = getOrCreateParentNode(CloudUtil.toDescriptor(category));
		DiffResult result = new DiffResult(index.get(category.getRefId()));
		Node node = new Node(parent, result);
		parent.getChildren().add(node);
		nodes.put(category.getRefId(), node);
		return node;
	}

	private Node createNodeFromDiff(DiffResult result) {
		Node parent = getOrCreateParentNode(result.getDescriptor());
		Node node = new Node(parent, result);
		parent.getChildren().add(node);
		nodes.put(result.getDescriptor().getRefId(), node);
		return node;
	}

	private Node getOrCreateModelTypeNode(ModelType type) {
		Node typeNode = nodes.get(type.name());
		if (typeNode != null)
			return typeNode;
		Node root = nodes.get("");
		typeNode = new Node(root, type);
		root.getChildren().add(typeNode);
		nodes.put(type.name(), typeNode);
		return typeNode;
	}

	public static class Node {

		private final Object content;
		private final Node parent;
		private final List<Node> children = new ArrayList<>();

		public Node(Node parent, Object content) {
			this.content = content;
			this.parent = parent;
		}

		public Object getContent() {
			return content;
		}

		public Node getParent() {
			return parent;
		}

		public List<Node> getChildren() {
			return children;
		}

	}

}
