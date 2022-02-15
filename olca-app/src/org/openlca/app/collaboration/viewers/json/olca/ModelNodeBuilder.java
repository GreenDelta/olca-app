package org.openlca.app.collaboration.viewers.json.olca;

import java.util.Comparator;

import org.openlca.app.collaboration.viewers.json.content.JsonNode;
import org.openlca.app.collaboration.viewers.json.content.JsonNodeBuilder;

import com.google.gson.JsonElement;

public class ModelNodeBuilder extends JsonNodeBuilder implements Comparator<JsonNode> {

	public ModelNodeBuilder() {
		super(ModelUtil.ELEMENT_FINDER);
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
		var o1 = getOrdinal(n1);
		var o2 = getOrdinal(n2);
		return Integer.compare(o1, o2);
	}

	private int getOrdinal(JsonNode node) {
		if (node.parent == null)
			return 0;
		var parent = node.parent.element();
		var type = ModelUtil.getType(parent);
		if (node.parent.element().isJsonArray() && node.element().isJsonObject()) {
			var obj = node.element().getAsJsonObject();
			if (obj.has("position")) {
				return obj.get("position").getAsInt();
			}
		}
		return PropertyLabels.getOrdinal(type, node.property);
	}

}
