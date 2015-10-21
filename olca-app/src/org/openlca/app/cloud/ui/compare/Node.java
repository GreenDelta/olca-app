package org.openlca.app.cloud.ui.compare;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

class Node {

	String key;
	Node parent;
	List<Node> children = new ArrayList<>();
	private JsonElement localElement;
	private JsonElement remoteElement;
	private JsonElement originalElement;

	static Node create(Node parent, String key, JsonElement local,
			JsonElement remote, JsonElement merged) {
		if (merged == null)
			return create(parent, key, local, remote);
		return new Node(parent, key, merged, remote, local);
	}

	static Node create(Node parent, String key, JsonElement local,
			JsonElement remote) {
		JsonElement original = local != null ? JsonUtil.deepCopy(local) : null;
		return new Node(parent, key, local, remote, original);
	}

	private Node(Node parent, String key, JsonElement localElement,
			JsonElement remoteElement, JsonElement originalElement) {
		this.parent = parent;
		this.key = key;
		this.localElement = localElement;
		this.remoteElement = remoteElement;
		this.originalElement = originalElement;
	}

	JsonElement getElement() {
		if (localElement != null)
			return localElement;
		return remoteElement;
	}

	JsonElement getElement(boolean local) {
		if (local)
			return localElement;
		return remoteElement;
	}

	boolean hasEqualValues() {
		return JsonUtil.equal(localElement, remoteElement);
	}

	void reset() {
		setValue(originalElement);
	}

	void copyRemoteValue() {
		setValue(remoteElement);
	}

	private void setValue(JsonElement toSet) {
		if (parent != null) {
			JsonElement parentElement = parent.localElement;
			if (parentElement.isJsonObject())
				parentElement.getAsJsonObject().add(key, toSet);
			else if (parentElement.isJsonArray()) {
				JsonObject arrayParent = parent.parent.localElement
						.getAsJsonObject();
				int positionInArray = Integer.parseInt(key);
				int i = 0;
				JsonArray newArray = parentElement.getAsJsonArray();
				Iterator<JsonElement> elements = parentElement.getAsJsonArray()
						.iterator();
				while (elements.hasNext()) {
					JsonElement next = elements.next();
					if (i++ != positionInArray)
						newArray.add(next);
					else
						newArray.add(toSet);
				}
				arrayParent.add(parent.key, newArray);
			}
		}
		this.localElement = toSet;
		updateChildren();
	}

	private void updateChildren() {
		if (children.isEmpty())
			return;
		for (Node child : children) {
			JsonElement element = null;
			if (localElement.isJsonObject()) {
				element = localElement.getAsJsonObject().get(child.key);
				if (element == null)
					element = JsonNull.INSTANCE;
			} else if (localElement.isJsonArray()) {
				int index = Integer.parseInt(child.key) - 1;
				element = localElement.getAsJsonArray().get(index);
				// TODO if local != remote
			}
			child.localElement = element;
			child.updateChildren();
		}
	}

}