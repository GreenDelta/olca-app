package org.openlca.app.cloud.ui.compare;

import java.util.Comparator;

import org.openlca.app.cloud.ui.compare.json.JsonNode;
import org.openlca.app.cloud.ui.compare.json.JsonNodeBuilder;

import com.google.gson.JsonElement;

public class ModelNodeBuilder extends JsonNodeBuilder implements
		Comparator<JsonNode> {

	public ModelNodeBuilder() {
		super(ModelUtil.getElementFinder());
	}

	@Override
	protected boolean skip(JsonElement parent, String property) {
		return !ModelUtil.displayElement(parent, property);
	}

	@Override
	protected boolean skipChildren(JsonElement parent, JsonElement element) {
		return ModelUtil.isReference(parent, element);
	}

	@Override
	protected boolean isReadOnly(JsonNode node, String property) {
		return ModelUtil.isReadOnly(node, property);
	}

	@Override
	public int compare(JsonNode n1, JsonNode n2) {
		int o1 = getOrdinal(n1);
		int o2 = getOrdinal(n2);
		return Integer.compare(o1, o2);
	}

	private int getOrdinal(JsonNode node) {
		if (node.parent == null)
			return 0;
		JsonElement parent = node.parent.getElement();
		String type = ModelUtil.getType(parent);
		return PropertyLabels.getOrdinal(type, node.property);
	}

}
