package org.openlca.app.collaboration.viewers.json.content;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jgit.diff.DiffEntry.Side;
import org.openlca.app.collaboration.util.Json;
import org.openlca.app.collaboration.util.Json.ElementFinder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public abstract class JsonNodeBuilder implements Comparator<JsonNode> {

	private ElementFinder elementFinder;

	public JsonNodeBuilder(ElementFinder elementFinder) {
		this.elementFinder = elementFinder;
	}

	public JsonNode build(JsonElement leftJson, JsonElement rightJson) {
		var node = JsonNode.createEditable(null, null, leftJson, rightJson, elementFinder);
		build(node, leftJson, rightJson);
		sort(node);
		return node;
	}

	private void build(JsonNode node, JsonElement leftValue, JsonElement rightValue) {
		if (leftValue != null) {
			build(node, leftValue, rightValue, Side.OLD);
		} else if (rightValue != null) {
			build(node, leftValue, rightValue, Side.NEW);
		}
	}

	private void build(JsonNode node, JsonElement leftValue, JsonElement rightValue, Side side) {
		var toCheck = side == Side.OLD ? leftValue : rightValue;
		if (toCheck.isJsonObject()) {
			build(node, Json.toJsonObject(leftValue), Json.toJsonObject(rightValue));
		}
		if (toCheck.isJsonArray()) {
			build(node, Json.toJsonArray(leftValue), Json.toJsonArray(rightValue));
		}
	}

	private void build(JsonNode node, JsonObject leftValue, JsonObject rightValue) {
		var added = new HashSet<String>();
		if (leftValue != null) {
			buildChildren(node, leftValue, rightValue, added, Side.OLD);
		}
		if (rightValue != null) {
			buildChildren(node, rightValue, leftValue, added, Side.NEW);
		}
	}

	private void buildChildren(JsonNode node, JsonObject json, JsonObject other, Set<String> added, Side side) {
		json.entrySet().stream()
				.filter(child -> side == Side.OLD || !added.contains(child.getKey()))
				.forEach(child -> {
					JsonElement otherValue = null;
					if (other != null) {
						otherValue = other.get(child.getKey());
					}
					if (side == Side.OLD) {
						build(node, child.getKey(), child.getValue(), otherValue);
						added.add(child.getKey());
					} else {
						build(node, child.getKey(), otherValue, child.getValue());
					}
				});
	}

	private void build(JsonNode node, JsonArray leftValue, JsonArray rightValue) {
		if (isReadOnly(node))
			return;
		var added = new HashSet<Integer>();
		if (leftValue != null) {
			buildChildren(node, leftValue, rightValue, Side.OLD, added);
		}
		if (rightValue != null) {
			buildChildren(node, rightValue, leftValue, Side.NEW, added);
		}
	}

	private void buildChildren(JsonNode node, JsonArray array, JsonArray otherArray, Side side, Set<Integer> added) {
		var count = 0;
		var counter = node.children.size() + 1;
		for (var value : array) {
			if (side == Side.NEW && added.contains(count++))
				continue;
			JsonElement otherValue = null;
			var index = elementFinder.find(node.property, value, otherArray, added);
			if (side == Side.OLD && index != -1) {
				otherValue = otherArray.get(index);
				added.add(index);
			}
			var leftValue = side == Side.OLD ? value : otherValue;
			var rightValue = side == Side.OLD ? otherValue : value;
			var property = Integer.toString(counter++);
			var childNode = isReadOnly(node)
					? JsonNode.createReadOnly(node, property, leftValue, rightValue, elementFinder)
					: JsonNode.createEditable(node, property, leftValue, rightValue, elementFinder);
			build(childNode, leftValue, rightValue);
			node.children.add(childNode);
		}
	}

	private JsonNode build(JsonNode parent, String property, JsonElement leftValue, JsonElement rightValue) {
		if (skip(parent, property))
			return null;
		var childNode = isReadOnly(parent)
				? JsonNode.createReadOnly(parent, property, leftValue, rightValue, elementFinder)
				: JsonNode.createEditable(parent, property, leftValue, rightValue, elementFinder);
		parent.children.add(childNode);
		build(childNode, leftValue, rightValue);
		return childNode;
	}

	private void sort(JsonNode node) {
		Collections.sort(node.children, this);
		node.children.forEach(child -> sort(child));
	}

	protected abstract boolean skip(JsonNode parent, String property);

	protected abstract boolean isReadOnly(JsonNode node);

}
