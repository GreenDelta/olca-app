package org.openlca.app.cloud.ui.compare;

import java.util.Collections;
import java.util.Comparator;

import com.google.gson.JsonElement;

class NodeSorter implements Comparator<JsonNode> {

	void sort(JsonNode node) {
		Collections.sort(node.children, this);
		for (JsonNode child : node.children)
			sort(child);
	}

	@Override
	public int compare(JsonNode n1, JsonNode n2) {
		int i1 = toInt(n1);
		int i2 = toInt(n2);
		if (i1 == i2)
			return compare(n1.key, n2.key);
		return i1 - i2;
	}

	private int compare(String key1, String key2) {
		try {
			return Integer.compare(Integer.parseInt(key1),
					Integer.parseInt(key2));
		} catch (NumberFormatException e) {
			return key1.compareTo(key2);
		}
	}

	private int toInt(JsonNode node) {
		JsonElement element = node.getElement();
		if (element == null)
			return 0;
		if (element.isJsonNull())
			return 1;
		if (element.isJsonPrimitive())
			return 1;
		if (element.isJsonObject()) {
			JsonNode parentNode = node.parent;
			if (parentNode.getElement().isJsonArray())
				parentNode = parentNode.parent;
			JsonElement parent = parentNode.getElement();
			if (JsonUtil.isReference(parent, element))
				return 1;
			else
				return 2;
		}
		return 3;
	}
}
