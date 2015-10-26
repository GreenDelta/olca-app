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
		// TODO sort by content, so array order matches
		Iterator<JsonElement> it1 = null;
		if (localArray != null)
			it1 = localArray.iterator();
		Iterator<JsonElement> it2 = null;
		if (remoteArray != null)
			it2 = remoteArray.iterator();
		int counter = 1;
		if (it1 != null)
			while (it1.hasNext()) {
				JsonElement localValue = it1.next();
				JsonElement remoteValue = it2 != null ? it2.hasNext() ? it2
						.next() : null : null;
				JsonNode childNode = JsonNode.create(node,
						Integer.toString(counter++), localValue, remoteValue);
				build(childNode, localValue, remoteValue);
				node.children.add(childNode);
			}
		if (it2 != null)
			while (it2.hasNext()) {
				JsonElement localValue = it1 != null ? it1.hasNext() ? it1
						.next() : null : null;
				JsonElement remoteValue = it2.next();
				JsonNode childNode = JsonNode.create(node,
						Integer.toString(counter++), localValue, remoteValue);
				build(childNode, localValue, remoteValue);
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
