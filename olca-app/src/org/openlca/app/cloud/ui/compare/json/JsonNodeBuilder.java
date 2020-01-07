package org.openlca.app.cloud.ui.compare.json;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.openlca.app.cloud.ui.compare.json.JsonUtil.ElementFinder;
import org.openlca.app.cloud.ui.diff.Site;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public abstract class JsonNodeBuilder implements Comparator<JsonNode> {

	private ElementFinder elementFinder;

	public JsonNodeBuilder(ElementFinder elementFinder) {
		this.elementFinder = elementFinder;
	}

	public JsonNode build(JsonElement leftJson, JsonElement rightJson) {
		JsonNode node = JsonNode.create(null, null, leftJson, rightJson, elementFinder, false);
		build(node, leftJson, rightJson);
		sort(node);
		return node;
	}

	private void build(JsonNode node, JsonElement left, JsonElement right) {
		if (left != null) {
			build(node, left, right, Site.LOCAL);
		} else if (right != null) {
			build(node, left, right, Site.REMOTE);
		}
	}

	private void build(JsonNode node, JsonElement left, JsonElement right, Site site) {
		JsonElement toCheck = site == Site.LOCAL ? left : right;
		if (toCheck.isJsonObject()) {
			build(node, JsonUtil.toJsonObject(left), JsonUtil.toJsonObject(right));
		}
		if (toCheck.isJsonArray()) {
			build(node, JsonUtil.toJsonArray(left), JsonUtil.toJsonArray(right));
		}
	}

	private void build(JsonNode node, JsonObject left, JsonObject right) {
		Set<String> added = new HashSet<>();
		if (left != null) {
			buildChildren(node, left, right, added, Site.LOCAL);
		}
		if (right != null) {
			buildChildren(node, right, left, added, Site.REMOTE);
		}
	}

	private void buildChildren(JsonNode node, JsonObject json, JsonObject other, Set<String> added, Site site) {
		for (Entry<String, JsonElement> child : json.entrySet()) {
			if (site == Site.REMOTE && added.contains(child.getKey()))
				continue;
			JsonElement otherValue = null;
			if (other != null) {
				otherValue = other.get(child.getKey());
			}
			if (site == Site.LOCAL) {
				build(node, child.getKey(), child.getValue(), otherValue);
				added.add(child.getKey());
			} else {
				build(node, child.getKey(), otherValue, child.getValue());
			}
		}
	}

	private void build(JsonNode node, JsonArray left, JsonArray right) {
		if (isReadOnly(node, node.property))
			return;
		Set<Integer> added = new HashSet<>();
		if (left != null) {
			buildChildren(node, left, right, Site.LOCAL, added);
		}
		if (right != null) {
			buildChildren(node, right, left, Site.REMOTE, added);
		}
	}

	private void buildChildren(JsonNode node, JsonArray array, JsonArray otherArray, Site site, Set<Integer> added) {
		int count = 0;
		int counter = node.children.size() + 1;
		for (JsonElement value : array) {
			if (site == Site.REMOTE && added.contains(count++))
				continue;
			JsonElement otherValue = null;
			int index = elementFinder.find(node.property, value, otherArray, added);
			if (site == Site.LOCAL && index != -1) {
				otherValue = otherArray.get(index);
				added.add(index);
			}
			JsonElement left = site == Site.LOCAL ? value : otherValue;
			JsonElement right = site == Site.LOCAL ? otherValue : value;
			String property = Integer.toString(counter++);
			JsonElement parent = node.parent.getElement(site);
			boolean readOnly = isReadOnly(node, node.property);
			JsonNode childNode = JsonNode.create(node, property, left, right, elementFinder, readOnly);
			if (!skipChildren(parent, value)) {
				build(childNode, left, right);
			}
			node.children.add(childNode);
		}
	}

	private JsonNode build(JsonNode parent, String property, JsonElement leftValue, JsonElement rightValue) {
		if (skip(parent.getElement(), property))
			return null;
		boolean readOnly = isReadOnly(parent, property);
		JsonNode childNode = JsonNode.create(parent, property, leftValue, rightValue, elementFinder, readOnly);
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
		for (JsonNode child : node.children) {
			sort(child);
		}
	}

	protected abstract boolean skip(JsonElement parent, String property);

	protected abstract boolean skipChildren(JsonElement parent, JsonElement element);

	protected abstract boolean isReadOnly(JsonNode node, String property);

}
