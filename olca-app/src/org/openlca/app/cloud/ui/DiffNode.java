package org.openlca.app.cloud.ui;

import java.util.ArrayList;
import java.util.List;

import org.openlca.core.model.ModelType;

public class DiffNode {

	final Object content;
	final DiffNode parent;
	final List<DiffNode> children = new ArrayList<>();

	DiffNode(DiffNode parent, Object content) {
		this.content = content;
		this.parent = parent;
	}

	boolean isModelTypeNode() {
		return content instanceof ModelType;
	}

	boolean isCategoryNode() {
		return getModelType() == ModelType.CATEGORY;
	}

	boolean isModelNode() {
		if (isModelTypeNode())
			return false;
		return getModelType() != ModelType.CATEGORY;
	}

	private ModelType getModelType() {
		if (isModelTypeNode())
			return null;
		DiffResult result = (DiffResult) content;
		return result.getDataset().getType();
	}

}