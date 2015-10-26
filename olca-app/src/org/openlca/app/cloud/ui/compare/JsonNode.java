package org.openlca.app.cloud.ui.compare;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

public class JsonNode {

	String key;
	JsonNode parent;
	List<JsonNode> children = new ArrayList<>();
	private JsonElement localElement;
	private JsonElement remoteElement;
	private JsonElement originalElement;

	static JsonNode create(JsonNode parent, String key, JsonElement local,
			JsonElement remote) {
		JsonElement original = local != null ? JsonUtil.deepCopy(local) : null;
		return new JsonNode(parent, key, local, remote, original);
	}

	private JsonNode(JsonNode parent, String key, JsonElement localElement,
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

	public JsonElement getLocalElement() {
		return localElement;
	}

	public JsonElement getRemoteElement() {
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
		for (JsonNode child : children) {
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