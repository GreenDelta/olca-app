package org.openlca.app.collaboration.viewers.json.olca;

import java.util.Comparator;

import org.openlca.app.collaboration.viewers.json.content.JsonNode;
import org.openlca.app.collaboration.viewers.json.content.JsonNodeBuilder;

public class ModelNodeBuilder extends JsonNodeBuilder implements Comparator<JsonNode> {

	public ModelNodeBuilder() {
		super(ModelUtil.ELEMENT_FINDER);
	}

	@Override
	protected boolean skip(JsonNode node, String property) {
		return !ModelUtil.displayElement(node, property);
	}

	@Override
	protected boolean isReadOnly(JsonNode node) {
		return ModelUtil.isReadOnly(node);
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
		if (node.parent.element().isJsonArray() && node.element().isJsonObject()) {
			var obj = node.element().getAsJsonObject();
			if (obj.has("position")) {
				return obj.get("position").getAsInt();
			}
		}
		return PropertyLabels.getOrdinal(node);
	}

}
