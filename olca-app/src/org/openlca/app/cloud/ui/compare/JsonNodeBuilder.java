package org.openlca.app.cloud.ui.compare;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonNodeBuilder {

	public JsonNode build(JsonElement localJson, JsonElement remoteJson) {
		JsonNode node = JsonNode.create(null, null, localJson, remoteJson);
		build(node, localJson, remoteJson);
		new NodeSorter().sort(node);
		return node;
	}

	private void build(JsonNode node, JsonElement local, JsonElement remote) {
		if (local != null)
			build(node, local, remote, true);
		else if (remote != null)
			build(node, local, remote, false);
	}

	private void build(JsonNode node, JsonElement local, JsonElement remote,
			boolean forLocal) {
		JsonElement toCheck = forLocal ? local : remote;
		if (toCheck.isJsonObject())
			build(node, JsonUtil.toJsonObject(local),
					JsonUtil.toJsonObject(remote));
		if (toCheck.isJsonArray())
			build(node, JsonUtil.toJsonArray(local),
					JsonUtil.toJsonArray(remote));
	}

	private void build(JsonNode node, JsonObject local, JsonObject remote) {
		Set<String> added = new HashSet<>();
		if (local != null)
			buildChildren(node, local, remote, added, true);
		if (remote != null)
			buildChildren(node, remote, local, added, false);
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

	private void build(JsonNode node, JsonArray local, JsonArray remote) {
		Set<Integer> added = new HashSet<>();
		if (local != null)
			buildChildren(node, local, remote, true, added);
		if (remote != null)
			buildChildren(node, remote, local, false, added);
	}

	private void buildChildren(JsonNode node, JsonArray array,
			JsonArray otherArray, boolean forLocal, Set<Integer> added) {
		int count = 0;
		int counter = node.children.size() + 1;
		for (JsonElement value : array) {
			if (!forLocal && added.contains(count++))
				continue;
			JsonElement otherValue = null;
			int index = JsonUtil.find(node.key, value, otherArray);
			if (forLocal && index != -1) {
				otherValue = otherArray.get(index);
				added.add(index);
			}
			JsonElement local = forLocal ? value : otherValue;
			JsonElement remote = forLocal ? otherValue : value;
			String key = Integer.toString(counter++);
			JsonNode childNode = JsonNode.create(node, key, local, remote);
			if (!JsonUtil.isReference(value))
				build(childNode, local, remote);
			node.children.add(childNode);
		}
	}

	private void build(JsonNode parent, String key, JsonElement localValue,
			JsonElement remoteValue) {
		if (!JsonUtil.displayElement(key))
			return;
		JsonNode childNode = JsonNode.create(parent, key, localValue,
				remoteValue);
		parent.children.add(childNode);
		if (localValue == null) {
			if (JsonUtil.isReference(remoteValue))
				return;
		} else if (JsonUtil.isReference(localValue))
			return;
		build(childNode, localValue, remoteValue);
	}

}
