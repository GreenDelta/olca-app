package org.openlca.app.collaboration.ui.viewers.json.content;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openlca.app.collaboration.ui.viewers.json.Side;
import org.openlca.app.collaboration.util.Json;
import org.openlca.app.collaboration.util.Json.ElementFinder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonNode {

	public final String property;
	public final JsonNode parent;
	public JsonElement localElement;
	public final JsonElement remoteElement;
	public final boolean readOnly;
	public final List<JsonNode> children = new ArrayList<>();
	public final JsonElement originalElement;
	private final ElementFinder elementFinder;

	static JsonNode create(JsonNode parent, String property, JsonElement left,
			JsonElement right, ElementFinder elementFinder, boolean readOnly) {
		var original = left != null ? Json.deepCopy(left) : null;
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

	public JsonElement element() {
		if (localElement != null)
			return localElement;
		return remoteElement;
	}

	public JsonElement element(Side side) {
		if (side == Side.LOCAL)
			return localElement;
		return remoteElement;
	}

	public boolean hasEqualValues() {
		return Json.equal(property, localElement, remoteElement, elementFinder);
	}

	public boolean hadDifferences() {
		return !Json.equal(property, localElement, originalElement, elementFinder);
	}

	public void setValue(JsonElement toSet, boolean leftToRight) {
		if (parent.localElement == null)
			return;
		var current = this.localElement;
		this.localElement = toSet;
		if (parent != null) {
			updateParent(toSet, current);
		}
		updateChildren(leftToRight);
	}

	private void updateParent(JsonElement toSet, JsonElement current) {
		var parentElement = parent.localElement;
		if (parentElement.isJsonObject()) {
			updateParent(parentElement.getAsJsonObject(), toSet, current);
		} else if (parentElement.isJsonArray()) {
			updateParent(parentElement.getAsJsonArray(), toSet, current);
		}
	}

	private void updateParent(JsonObject parentElement, JsonElement toSet, JsonElement current) {
		parentElement.add(property, toSet);
	}

	private void updateParent(JsonArray parentElement, JsonElement toSet, JsonElement current) {
		var arrayParent = parent.parent.localElement.getAsJsonObject();
		var array = parentElement.getAsJsonArray();
		if (toSet == null) {
			// remove
			var index = elementFinder.find(parent.property, current, array, null);
			array = Json.remove(index, array);
		} else {
			// add or replace
			var index = elementFinder.find(parent.property, toSet, array, null);
			if (index == -1) {
				array.add(toSet);
			} else {
				array = Json.replace(index, array, toSet);
			}
		}
		parent.localElement = array;
		arrayParent.add(parent.property, array);
	}

	private void updateChildren(boolean leftToRight) {
		if (children.isEmpty())
			return;
		var assigned = new HashSet<Integer>();
		children.forEach(child -> {
			var element = getElement(child, leftToRight, assigned);
			child.localElement = element;
			child.updateChildren(leftToRight);
		});
	}

	private JsonElement getElement(JsonNode node, boolean leftToRight, Set<Integer> assigned) {
		if (localElement == null)
			return null;
		if (localElement.isJsonObject())
			return localElement.getAsJsonObject().get(node.property);
		if (!localElement.isJsonArray())
			return null;
		var toFind = leftToRight
				? node.originalElement
				: node.remoteElement;
		var index = elementFinder.find(property, toFind, localElement.getAsJsonArray(), assigned);
		if (index == -1)
			return null;
		assigned.add(index);
		return localElement.getAsJsonArray().get(index);
	}

}