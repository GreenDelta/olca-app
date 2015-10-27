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

	private void build(JsonNode node, JsonElement localJson,
			JsonElement remoteJson) {
		if (localJson != null) {
			if (localJson.isJsonObject())
				build(node, JsonUtil.toJsonObject(localJson),
						JsonUtil.toJsonObject(remoteJson));
			if (localJson.isJsonArray())
				build(node, JsonUtil.toJsonArray(localJson),
						JsonUtil.toJsonArray(remoteJson));
		} else if (remoteJson != null) {
			if (remoteJson.isJsonObject())
				build(node, JsonUtil.toJsonObject(localJson),
						JsonUtil.toJsonObject(remoteJson));
			if (remoteJson.isJsonArray())
				build(node, JsonUtil.toJsonArray(localJson),
						JsonUtil.toJsonArray(remoteJson));
		}
	}

	private void build(JsonNode node, JsonObject localObject,
			JsonObject remoteObject) {
		Set<String> added = new HashSet<>();
		if (localObject != null)
			for (Entry<String, JsonElement> child : localObject.entrySet()) {
				JsonElement remoteValue = null;
				if (remoteObject != null)
					remoteValue = remoteObject.get(child.getKey());
				build(node, child.getKey(), child.getValue(), remoteValue);
				added.add(child.getKey());
			}
		if (remoteObject != null)
			for (Entry<String, JsonElement> child : remoteObject.entrySet()) {
				if (added.contains(child.getKey()))
					continue;
				JsonElement localValue = null;
				if (localObject != null)
					localValue = localObject.get(child.getKey());
				build(node, child.getKey(), localValue, child.getValue());
			}
	}

	private void build(JsonNode node, JsonArray localArray,
			JsonArray remoteArray) {
		Iterator<JsonElement> iterator = null;
		if (localArray != null)
			iterator = localArray.iterator();
		int counter = 1;
		Set<Integer> alreadyAddedRemotes = new HashSet<>();
		if (iterator != null)
			while (iterator.hasNext()) {
				JsonElement localValue = iterator.next();
				int index = JsonUtil.find(node.key, localValue, remoteArray);
				JsonElement remoteValue = null;
				if (index != -1) {
					remoteValue = remoteArray.get(index);
					alreadyAddedRemotes.add(index);
				}
				JsonNode childNode = JsonNode.create(node,
						Integer.toString(counter++), localValue, remoteValue);
				if (!JsonUtil.isReference(localValue))
					build(childNode, localValue, remoteValue);
				node.children.add(childNode);
			}
		if (remoteArray != null)
			iterator = remoteArray.iterator();
		if (iterator != null) {
			int count = 0;
			while (iterator.hasNext()) {
				JsonElement remoteValue = iterator.next();
				if (alreadyAddedRemotes.contains(count++))
					continue;
				int index = JsonUtil.find(node.key, remoteValue, localArray);
				JsonElement localValue = index != -1 ? localArray.get(index)
						: null;
				JsonNode childNode = JsonNode.create(node,
						Integer.toString(counter++), localValue, remoteValue);
				if (!JsonUtil.isReference(remoteValue))
					build(childNode, localValue, remoteValue);
				node.children.add(childNode);
			}
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
