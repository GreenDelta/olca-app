package org.openlca.app.collaboration.viewers.json.content;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.openlca.app.collaboration.util.Json;
import org.openlca.app.collaboration.util.Json.ElementFinder;
import org.openlca.app.collaboration.viewers.json.Side;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public abstract class JsonNodeBuilder implements Comparator<JsonNode> {

	private ElementFinder elementFinder;

	public JsonNodeBuilder(ElementFinder elementFinder) {
		this.elementFinder = elementFinder;
	}

	public JsonNode build(JsonElement leftJson, JsonElement rightJson) {
		var node = JsonNode.create(null, null, leftJson, rightJson, elementFinder, false);
		build(node, leftJson, rightJson);
		sort(node);
		return node;
	}

	private void build(JsonNode node, JsonElement left, JsonElement right) {
		if (left != null) {
			build(node, left, right, Side.LOCAL);
		} else if (right != null) {
			build(node, left, right, Side.REMOTE);
		}
	}

	private void build(JsonNode node, JsonElement left, JsonElement right, Side side) {
		var toCheck = side == Side.LOCAL ? left : right;
		if (toCheck.isJsonObject()) {
			build(node, Json.toJsonObject(left), Json.toJsonObject(right));
		}
		if (toCheck.isJsonArray()) {
			build(node, Json.toJsonArray(left), Json.toJsonArray(right));
		}
	}

	private void build(JsonNode node, JsonObject left, JsonObject right) {
		var added = new HashSet<String>();
		if (left != null) {
			buildChildren(node, left, right, added, Side.LOCAL);
		}
		if (right != null) {
			buildChildren(node, right, left, added, Side.REMOTE);
		}
	}

	private void buildChildren(JsonNode node, JsonObject json, JsonObject other, Set<String> added, Side side) {
		json.entrySet().stream()
				.filter(child -> side == Side.LOCAL || !added.contains(child.getKey()))
				.forEach(child -> {
					JsonElement otherValue = null;
					if (other != null) {
						otherValue = other.get(child.getKey());
					}
					if (side == Side.LOCAL) {
						build(node, child.getKey(), child.getValue(), otherValue);
						added.add(child.getKey());
					} else {
						build(node, child.getKey(), otherValue, child.getValue());
					}
				});
	}

	private void build(JsonNode node, JsonArray left, JsonArray right) {
		if (isReadOnly(node, node.property))
			return;
		var added = new HashSet<Integer>();
		if (left != null) {
			buildChildren(node, left, right, Side.LOCAL, added);
		}
		if (right != null) {
			buildChildren(node, right, left, Side.REMOTE, added);
		}
	}

	private void buildChildren(JsonNode node, JsonArray array, JsonArray otherArray, Side side, Set<Integer> added) {
		var count = 0;
		var counter = node.children.size() + 1;
		for (var value : array) {
			if (side == Side.REMOTE && added.contains(count++))
				continue;
			JsonElement otherValue = null;
			var index = elementFinder.find(node.property, value, otherArray, added);
			if (side == Side.LOCAL && index != -1) {
				otherValue = otherArray.get(index);
				added.add(index);
			}
			var left = side == Side.LOCAL ? value : otherValue;
			var right = side == Side.LOCAL ? otherValue : value;
			var property = Integer.toString(counter++);
			var parent = node.parent.element(side);
			var readOnly = isReadOnly(node, node.property);
			var childNode = JsonNode.create(node, property, left, right, elementFinder, readOnly);
			if (!skipChildren(parent, value)) {
				build(childNode, left, right);
			}
			node.children.add(childNode);
		}
	}

	private JsonNode build(JsonNode parent, String property, JsonElement leftValue, JsonElement rightValue) {
		if (skip(parent.element(), property))
			return null;
		var readOnly = isReadOnly(parent, property);
		var childNode = JsonNode.create(parent, property, leftValue, rightValue, elementFinder, readOnly);
		parent.children.add(childNode);
		if (leftValue == null) {
			if (skipChildren(parent.localElement, rightValue))
				return childNode;
		} else if (skipChildren(parent.remoteElement, leftValue))
			return childNode;
		build(childNode, leftValue, rightValue);
		return childNode;
	}

	private void sort(JsonNode node) {
		Collections.sort(node.children, this);
		node.children.forEach(child -> sort(child));
	}

	protected abstract boolean skip(JsonElement parent, String property);

	protected abstract boolean skipChildren(JsonElement parent, JsonElement element);

	protected abstract boolean isReadOnly(JsonNode node, String property);

}
