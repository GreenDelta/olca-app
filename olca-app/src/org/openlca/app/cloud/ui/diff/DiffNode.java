package org.openlca.app.cloud.ui.diff;

import java.util.ArrayList;
import java.util.List;

import org.openlca.app.cloud.ui.diff.DiffResult.DiffResponse;
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

	ModelType getModelType() {
		if (isModelTypeNode())
			return null;
		DiffResult result = (DiffResult) content;
		return result.getDataset().getType();
	}

	boolean hasChanged() {
		if (isModelTypeNode())
			return false;
		DiffResult result = (DiffResult) content;
		return result.getType() != DiffResponse.NONE;
	}

	public DiffResult getContent() {
		if (isModelTypeNode())
			return null;
		return (DiffResult) content;
	}

}