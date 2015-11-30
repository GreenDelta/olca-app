package org.openlca.app.cloud.ui.compare.json;

import java.util.ArrayList;
import java.util.List;

import org.openlca.app.cloud.ui.compare.json.JsonUtil.ElementFinder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonNode {

	public String property;
	public JsonNode parent;
	public List<JsonNode> children = new ArrayList<>();
	private JsonElement localElement;
	private JsonElement remoteElement;
	private JsonElement originalElement;
	private ElementFinder elementFinder;
	boolean readOnly;

	static JsonNode create(JsonNode parent, String property, JsonElement local,
			JsonElement remote, ElementFinder elementFinder, boolean readOnly) {
		JsonElement original = local != null ? JsonUtil.deepCopy(local) : null;
		return new JsonNode(parent, property, local, remote, original,
				elementFinder, readOnly);
	}

	private JsonNode(JsonNode parent, String property,
			JsonElement localElement, JsonElement remoteElement,
			JsonElement originalElement, ElementFinder elementFinder,
			boolean readOnly) {
		this.parent = parent;
		this.property = property;
		this.localElement = localElement;
		this.remoteElement = remoteElement;
		this.originalElement = originalElement;
		this.elementFinder = elementFinder;
		this.readOnly = readOnly;
	}

	public JsonElement getElement() {
		if (localElement != null)
			return localElement;
		return remoteElement;
	}

	public JsonElement getElement(boolean local) {
		if (local)
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
		return JsonUtil.equal(property, localElement, remoteElement,
				elementFinder);
	}

	void reset() {
		setValue(originalElement, true);
	}

	void copyRemoteValue() {
		setValue(remoteElement, false);
	}

	private void setValue(JsonElement toSet, boolean isReset) {
		if (parent.localElement == null)
			return;
		JsonElement current = this.localElement;
		this.localElement = toSet;
		if (parent != null)
			updateParent(toSet, current);
		updateChildren(isReset);
	}

	private void updateParent(JsonElement toSet, JsonElement current) {
		JsonElement parentElement = parent.localElement;
		if (parentElement.isJsonObject())
			updateParent(parentElement.getAsJsonObject(), toSet, current);
		else if (parentElement.isJsonArray())
			updateParent(parentElement.getAsJsonArray(), toSet, current);
	}

	private void updateParent(JsonObject parentElement, JsonElement toSet,
			JsonElement current) {
		parentElement.add(property, toSet);
	}

	private void updateParent(JsonArray parentElement, JsonElement toSet,
			JsonElement current) {
		JsonObject arrayParent = parent.parent.localElement.getAsJsonObject();
		JsonArray array = parentElement.getAsJsonArray();
		if (toSet == null) {
			// remove
			int index = elementFinder.find(parent.property, current, array);
			array = JsonUtil.remove(index, array);
		} else {
			// add or replace
			int index = elementFinder.find(parent.property, toSet, array);
			if (index == -1)
				array.add(toSet);
			else
				array = JsonUtil.replace(index, array, toSet);
		}
		parent.localElement = array;
		arrayParent.add(parent.property, array);
	}

	private void updateChildren(boolean isReset) {
		if (children.isEmpty())
			return;
		for (JsonNode child : children) {
			JsonElement element = null;
			if (localElement != null)
				if (localElement.isJsonObject())
					element = localElement.getAsJsonObject()
							.get(child.property);
				else if (localElement.isJsonArray()) {
					JsonElement toFind = null;
					if (isReset)
						toFind = child.originalElement;
					else
						toFind = child.remoteElement;
					int index = elementFinder.find(property, toFind,
							localElement.getAsJsonArray());
					if (index != -1)
						element = localElement.getAsJsonArray().get(index);
				}
			child.localElement = element;
			child.updateChildren(isReset);
		}
	}
}