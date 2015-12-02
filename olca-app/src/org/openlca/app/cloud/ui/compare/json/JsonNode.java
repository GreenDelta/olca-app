package org.openlca.app.cloud.ui.compare.json;

import java.util.ArrayList;
import java.util.List;

import org.openlca.app.cloud.ui.compare.json.JsonUtil.ElementFinder;
import org.openlca.app.cloud.ui.compare.json.viewer.JsonTreeViewer.Side;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonNode {

	public String property;
	public JsonNode parent;
	public JsonElement leftElement;
	public JsonElement rightElement;
	public boolean readOnly;
	public List<JsonNode> children = new ArrayList<>();
	JsonElement originalElement;
	private ElementFinder elementFinder;

	static JsonNode create(JsonNode parent, String property, JsonElement left,
			JsonElement right, ElementFinder elementFinder, boolean readOnly) {
		JsonElement original = left != null ? JsonUtil.deepCopy(left) : null;
		return new JsonNode(parent, property, left, right, original,
				elementFinder, readOnly);
	}

	private JsonNode(JsonNode parent, String property, JsonElement leftElement,
			JsonElement rightElement, JsonElement originalElement,
			ElementFinder elementFinder, boolean readOnly) {
		this.parent = parent;
		this.property = property;
		this.leftElement = leftElement;
		this.rightElement = rightElement;
		this.originalElement = originalElement;
		this.elementFinder = elementFinder;
		this.readOnly = readOnly;
	}

	public JsonElement getElement() {
		if (leftElement != null)
			return leftElement;
		return rightElement;
	}

	public JsonElement getElement(Side side) {
		if (side == Side.LEFT)
			return leftElement;
		return rightElement;
	}

	public boolean hasEqualValues() {
		return JsonUtil.equal(property, leftElement, rightElement,
				elementFinder);
	}

	boolean hadDifferences() {
		return !JsonUtil.equal(property, leftElement, originalElement,
				elementFinder);
	}

	void setValue(JsonElement toSet, boolean leftToRight) {
		if (parent.leftElement == null)
			return;
		JsonElement current = this.leftElement;
		this.leftElement = toSet;
		if (parent != null)
			updateParent(toSet, current);
		updateChildren(leftToRight);
	}

	private void updateParent(JsonElement toSet, JsonElement current) {
		JsonElement parentElement = parent.leftElement;
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
		JsonObject arrayParent = parent.parent.leftElement.getAsJsonObject();
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
		parent.leftElement = array;
		arrayParent.add(parent.property, array);
	}

	private void updateChildren(boolean leftToRight) {
		if (children.isEmpty())
			return;
		for (JsonNode child : children) {
			JsonElement element = getElement(child, leftToRight);
			child.leftElement = element;
			child.updateChildren(leftToRight);
		}
	}

	private JsonElement getElement(JsonNode node, boolean leftToRight) {
		if (leftElement == null)
			return null;
		if (leftElement.isJsonObject())
			return leftElement.getAsJsonObject().get(node.property);
		if (!leftElement.isJsonArray())
			return null;
		JsonElement toFind = null;
		if (leftToRight)
			toFind = node.originalElement;
		else
			toFind = node.rightElement;
		int index = elementFinder.find(property, toFind,
				leftElement.getAsJsonArray());
		if (index == -1)
			return null;
		return leftElement.getAsJsonArray().get(index);
	}

}