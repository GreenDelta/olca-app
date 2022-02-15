package org.openlca.app.collaboration.viewers.diff;

import java.util.ArrayList;
import java.util.List;

import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ModelType;

public class DiffNode {

	final Object content;
	final DiffNode parent;
	public final List<DiffNode> children = new ArrayList<>();

	DiffNode(DiffNode parent, Object content) {
		this.parent = parent;
		this.content = content;
	}

	boolean isDatabaseNode() {
		return content instanceof IDatabase;
	}

	boolean isModelTypeNode() {
		return content instanceof ModelType;
	}

	boolean isCategoryNode() {
		return content instanceof String s && s.contains("/");
	}

	boolean isModelNode() {
		return content instanceof DiffResult;
	}

	ModelType getModelType() {
		if (isModelTypeNode())
			return (ModelType) content;
		if (isCategoryNode())
			return ModelType.valueOf(content.toString().substring(0, content.toString().indexOf("/")));
		if (content instanceof DiffResult result)
			return result.ref().type;
		return null;
	}

	boolean hasChanged() {
		if (!isModelNode())
			return false;
		return !contentAsDiffResult().noAction();
	}

	IDatabase contentAsDatabase() {
		return content instanceof IDatabase db ? db : null;
	}

	ModelType contentAsModelType() {
		return content instanceof ModelType t ? t : null;
	}

	String contentAsString() {
		return content instanceof String s ? s : null;
	}

	public DiffResult contentAsDiffResult() {
		return content instanceof DiffResult d ? d : null;
	}

}