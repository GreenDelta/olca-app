package org.openlca.app.collaboration.viewers.json.content;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.diff.DiffEntry.Side;
import org.openlca.app.collaboration.util.Json;
import org.openlca.app.collaboration.util.Json.ElementFinder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonNode {

	public final String property;
	public final JsonNode parent;
	public JsonElement left;
	public final JsonElement right;
	public final boolean readOnly;
	public final List<JsonNode> children = new ArrayList<>();
	public final JsonElement original;
	private final ElementFinder elementFinder;

	static JsonNode createReadOnly(JsonNode parent, String property, JsonElement left, JsonElement right,
			ElementFinder elementFinder) {
		return new JsonNode(parent, property, left, right, elementFinder, true);
	}

	static JsonNode createEditable(JsonNode parent, String property, JsonElement left, JsonElement right,
			ElementFinder elementFinder) {
		return new JsonNode(parent, property, left, right, elementFinder, false);
	}

	private JsonNode(JsonNode parent, String property, JsonElement leftElement,
			JsonElement rightElement, ElementFinder elementFinder, boolean readOnly) {
		this.parent = parent;
		this.property = property;
		this.left = leftElement;
		this.right = rightElement;
		this.original = left != null ? Json.deepCopy(left) : null;
		this.elementFinder = elementFinder;
		this.readOnly = readOnly;
	}

	public JsonElement element() {
		if (left != null)
			return left;
		return right;
	}

	public JsonElement element(Side side) {
		if (side == Side.OLD)
			return left;
		return right;
	}

	public boolean hasEqualValues() {
		return Json.equal(property, left, right, elementFinder);
	}
	
	public boolean leftEqualsOriginal() {
		return Json.equal(property, left, original, elementFinder);		
	}

	public boolean hadDifferences() {
		return !Json.equal(property, left, original, elementFinder);
	}

	public void setValue(JsonElement toSet, boolean leftToRight) {
		if (parent.left == null)
			return;
		var current = this.left;
		this.left = toSet;
		if (parent != null) {
			updateParent(toSet, current);
		}
		updateChildren(leftToRight);
	}

	private void updateParent(JsonElement toSet, JsonElement current) {
		var parentElement = parent.left;
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
		var arrayParent = parent.parent.left.getAsJsonObject();
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
		parent.left = array;
		arrayParent.add(parent.property, array);
	}

	private void updateChildren(boolean leftToRight) {
		if (children.isEmpty())
			return;
		var assigned = new HashSet<Integer>();
		children.forEach(child -> {
			var element = getElement(child, leftToRight, assigned);
			child.left = element;
			child.updateChildren(leftToRight);
		});
	}

	private JsonElement getElement(JsonNode node, boolean leftToRight, Set<Integer> assigned) {
		if (left == null)
			return null;
		if (left.isJsonObject())
			return left.getAsJsonObject().get(node.property);
		if (!left.isJsonArray())
			return null;
		var toFind = leftToRight
				? node.original
				: node.right;
		var index = elementFinder.find(property, toFind, left.getAsJsonArray(), assigned);
		if (index == -1)
			return null;
		assigned.add(index);
		return left.getAsJsonArray().get(index);
	}

}