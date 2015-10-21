package org.openlca.app.cloud.ui.compare;

import java.util.Collections;
import java.util.Comparator;

import com.google.gson.JsonElement;

class NodeSorter implements Comparator<Node> {

	void sort(Node node) {
		Collections.sort(node.children, this);
		for (Node child : node.children)
			sort(child);
	}

	@Override
	public int compare(Node n1, Node n2) {
		int i1 = toInt(n1.getElement());
		int i2 = toInt(n2.getElement());
		if (i1 == i2)
			return n1.key.compareTo(n2.key);
		return i1 - i2;
	}

	private int toInt(JsonElement element) {
		if (element == null)
			return 0;
		if (element.isJsonNull())
			return 1;
		if (element.isJsonPrimitive())
			return 1;
		if (element.isJsonObject())
			if (JsonUtil.isReference(element.getAsJsonObject()))
				return 1;
			else
				return 2;
		return 3;
	}
}
