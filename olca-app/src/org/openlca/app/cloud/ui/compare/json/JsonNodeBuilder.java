package org.openlca.app.cloud.ui.compare.json;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.openlca.app.cloud.ui.compare.json.JsonUtil.ElementFinder;
import org.openlca.app.cloud.ui.compare.json.viewer.JsonTreeViewer.Side;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public abstract class JsonNodeBuilder implements Comparator<JsonNode> {

	private ElementFinder elementFinder;

	public JsonNodeBuilder(ElementFinder elementFinder) {
		this.elementFinder = elementFinder;
	}

	public JsonNode build(JsonElement leftJson, JsonElement rightJson) {
		JsonNode node = JsonNode.create(null, null, leftJson, rightJson,
				elementFinder, false);
		build(node, leftJson, rightJson);
		sort(node);
		return node;
	}

	private void build(JsonNode node, JsonElement left, JsonElement right) {
		if (left != null)
			build(node, left, right, Side.LEFT);
		else if (right != null)
			build(node, left, right, Side.RIGHT);
	}

	private void build(JsonNode node, JsonElement left, JsonElement right,
			Side side) {
		JsonElement toCheck = side == Side.LEFT ? left : right;
		if (toCheck.isJsonObject())
			build(node, JsonUtil.toJsonObject(left),
					JsonUtil.toJsonObject(right));
		if (toCheck.isJsonArray())
			build(node, JsonUtil.toJsonArray(left), JsonUtil.toJsonArray(right));
	}

	private void build(JsonNode node, JsonObject left, JsonObject right) {
		Set<String> added = new HashSet<>();
		if (left != null)
			buildChildren(node, left, right, added, true);
		if (right != null)
			buildChildren(node, right, left, added, false);
	}

	private void buildChildren(JsonNode node, JsonObject json,
			JsonObject other, Set<String> added, boolean forLocal) {
		for (Entry<String, JsonElement> child : json.entrySet()) {
			if (!forLocal && added.contains(child.getKey()))
				continue;
			JsonElement otherValue = null;
			if (other != null)
				otherValue = other.get(child.getKey());
			if (forLocal) {
				build(node, child.getKey(), child.getValue(), otherValue);
				added.add(child.getKey());
			} else
				build(node, child.getKey(), otherValue, child.getValue());
		}
	}

	private void build(JsonNode node, JsonArray left, JsonArray right) {
		Set<Integer> added = new HashSet<>();
		if (left != null)
			buildChildren(node, left, right, Side.LEFT, added);
		if (right != null)
			buildChildren(node, right, left, Side.RIGHT, added);
	}

	private void buildChildren(JsonNode node, JsonArray array,
			JsonArray otherArray, Side side, Set<Integer> added) {
		int count = 0;
		int counter = node.children.size() + 1;
		for (JsonElement value : array) {
			if (side == Side.RIGHT && added.contains(count++))
				continue;
			JsonElement otherValue = null;
			int index = elementFinder.find(node.property, value, otherArray);
			if (side == Side.LEFT && index != -1) {
				otherValue = otherArray.get(index);
				added.add(index);
			}
			JsonElement left = side == Side.LEFT ? value : otherValue;
			JsonElement right = side == Side.LEFT ? otherValue : value;
			String property = Integer.toString(counter++);
			JsonElement parent = node.parent.getElement(side);
			JsonNode childNode = JsonNode.create(node, property, left, right,
					elementFinder, isReadOnly(node, node.property));
			if (!skipChildren(parent, value))
				build(childNode, left, right);
			node.children.add(childNode);
		}
	}

	private JsonNode build(JsonNode parent, String property,
			JsonElement leftValue, JsonElement rightValue) {
		if (skip(parent.getElement(), property))
			return null;
		JsonNode childNode = JsonNode.create(parent, property, leftValue,
				rightValue, elementFinder, isReadOnly(parent, property));
		parent.children.add(childNode);
		if (leftValue == null) {
			if (skipChildren(parent.leftElement, rightValue))
				return childNode;
		} else if (skipChildren(parent.rightElement, leftValue))
			return childNode;
		build(childNode, leftValue, rightValue);
		return childNode;
	}

	private void sort(JsonNode node) {
		Collections.sort(node.children, this);
		for (JsonNode child : node.children)
			sort(child);
	}

	protected abstract boolean skip(JsonElement parent, String property);

	protected abstract boolean skipChildren(JsonElement parent,
			JsonElement element);

	protected abstract boolean isReadOnly(JsonNode node, String property);

}
