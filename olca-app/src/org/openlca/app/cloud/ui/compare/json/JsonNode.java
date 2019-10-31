package org.openlca.app.cloud.ui.compare.json;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openlca.app.cloud.ui.compare.json.JsonUtil.ElementFinder;
import org.openlca.app.cloud.ui.diff.Site;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonNode {

	public String property;
	public JsonNode parent;
	public JsonElement localElement;
	public JsonElement remoteElement;
	public boolean readOnly;
	public List<JsonNode> children = new ArrayList<>();
	JsonElement originalElement;
	private ElementFinder elementFinder;

	static JsonNode create(JsonNode parent, String property, JsonElement left,
			JsonElement right, ElementFinder elementFinder, boolean readOnly) {
		JsonElement original = left != null ? JsonUtil.deepCopy(left) : null;
		return new JsonNode(parent, property, left, right, original, elementFinder, readOnly);
	}

	private JsonNode(JsonNode parent, String property, JsonElement leftElement,
			JsonElement rightElement, JsonElement originalElement,
			ElementFinder elementFinder, boolean readOnly) {
		this.parent = parent;
		this.property = property;
		this.localElement = leftElement;
		this.remoteElement = rightElement;
		this.originalElement = originalElement;
		this.elementFinder = elementFinder;
		this.readOnly = readOnly;
	}

	public JsonElement getElement() {
		if (localElement != null)
			return localElement;
		return remoteElement;
	}

	public JsonElement getElement(Site site) {
		if (site == Site.LOCAL)
			return localElement;
		return remoteElement;
	}

	public boolean hasEqualValues() {
		return JsonUtil.equal(property, localElement, remoteElement, elementFinder);
	}

	boolean hadDifferences() {
		return !JsonUtil.equal(property, localElement, originalElement, elementFinder);
	}

	void setValue(JsonElement toSet, boolean leftToRight) {
		if (parent.localElement == null)
			return;
		JsonElement current = this.localElement;
		this.localElement = toSet;
		if (parent != null)
			updateParent(toSet, current);
		updateChildren(leftToRight);
	}

	private void updateParent(JsonElement toSet, JsonElement current) {
		JsonElement parentElement = parent.localElement;
		if (parentElement.isJsonObject())
			updateParent(parentElement.getAsJsonObject(), toSet, current);
		else if (parentElement.isJsonArray())
			updateParent(parentElement.getAsJsonArray(), toSet, current);
	}

	private void updateParent(JsonObject parentElement, JsonElement toSet, JsonElement current) {
		parentElement.add(property, toSet);
	}

	private void updateParent(JsonArray parentElement, JsonElement toSet, JsonElement current) {
		JsonObject arrayParent = parent.parent.localElement.getAsJsonObject();
		JsonArray array = parentElement.getAsJsonArray();
		if (toSet == null) {
			// remove
			int index = elementFinder.find(parent.property, current, array, null);
			array = JsonUtil.remove(index, array);
		} else {
			// add or replace
			int index = elementFinder.find(parent.property, toSet, array, null);
			if (index == -1)
				array.add(toSet);
			else
				array = JsonUtil.replace(index, array, toSet);
		}
		parent.localElement = array;
		arrayParent.add(parent.property, array);
	}

	private void updateChildren(boolean leftToRight) {
		if (children.isEmpty())
			return;
		Set<Integer> assigned = new HashSet<>();
		for (JsonNode child : children) {
			JsonElement element = getElement(child, leftToRight, assigned);
			child.localElement = element;
			child.updateChildren(leftToRight);
		}
	}

	private JsonElement getElement(JsonNode node, boolean leftToRight, Set<Integer> assigned) {
		if (localElement == null)
			return null;
		if (localElement.isJsonObject())
			return localElement.getAsJsonObject().get(node.property);
		if (!localElement.isJsonArray())
			return null;
		JsonElement toFind = null;
		if (leftToRight)
			toFind = node.originalElement;
		else
			toFind = node.remoteElement;
		int index = elementFinder.find(property, toFind, localElement.getAsJsonArray(), assigned);
		if (index == -1)
			return null;
		assigned.add(index);
		return localElement.getAsJsonArray().get(index);
	}

}