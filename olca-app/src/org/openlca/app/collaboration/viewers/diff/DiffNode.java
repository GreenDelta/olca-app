package org.openlca.app.collaboration.viewers.diff;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
		return content instanceof TriDiff;
	}

	ModelType getModelType() {
		if (isModelTypeNode())
			return (ModelType) content;
		if (isCategoryNode())
			return ModelType.valueOf(content.toString().substring(0, content.toString().indexOf("/")));
		if (content instanceof TriDiff diff)
			return diff.type;
		return null;
	}

	boolean hasChanged() {
		if (!isModelNode())
			return false;
		return !contentAsTriDiff().noAction();
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

	public TriDiff contentAsTriDiff() {
		return content instanceof TriDiff d ? d : null;
	}

	@Override
	public final int hashCode() {
		if (content == null)
			return super.hashCode();
		return parent == null
				? content.hashCode()
				: Objects.hash(content, parent);
	}

	@Override
	public final boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof DiffNode))
			return false;
		var other = (DiffNode) o;
		return Objects.equals(this.content, other.content)
				&& Objects.equals(this.parent, other.parent);
	}

}