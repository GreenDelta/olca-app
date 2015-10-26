package org.openlca.app.cloud.ui.compare;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

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
				int index = find(node.key, localValue, remoteArray);
				JsonElement remoteValue = null;
				if (index != -1) {
					remoteValue = remoteArray.get(index);
					alreadyAddedRemotes.add(index);
				}
				JsonNode childNode = JsonNode.create(node,
						Integer.toString(counter++), localValue, remoteValue);
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
				int index = find(node.key, remoteValue, localArray);
				JsonElement localValue = index != -1 ? localArray.get(index)
						: null;
				JsonNode childNode = JsonNode.create(node,
						Integer.toString(counter++), localValue, remoteValue);
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

	private int find(String key, JsonElement element, JsonArray array) {
		if (key.equals("units"))
			return find(element, array, "name");
		if (key.equals("flowProperties"))
			return find(element, array, "flowProperty.@id");
		if (key.equals("exchanges"))
			return find(element, array, "flow.@id", "input");
		return find(element, array, "@id");
	}

	private int find(JsonElement element, JsonArray array, String... fields) {
		if (fields == null)
			return -1;
		JsonObject object = element.getAsJsonObject();
		String[] values = getValues(object, fields);
		if (values == null)
			return -1;
		Iterator<JsonElement> iterator = array.iterator();
		int index = 0;
		while (iterator.hasNext()) {
			JsonObject other = iterator.next().getAsJsonObject();
			String[] otherValues = getValues(other, fields);
			if (equal(values, otherValues))
				return index;
			index++;
		}
		return -1;
	}

	private String get(JsonObject object, String... path) {
		if (path == null)
			return null;
		if (path.length == 0)
			return null;
		Stack<String> stack = new Stack<>();
		for (int i = path.length - 1; i >= 0; i--)
			stack.add(path[i]);
		while (stack.size() > 1) {
			String next = stack.pop();
			if (!object.has(next))
				return null;
			object = object.get(next).getAsJsonObject();
		}
		JsonElement value = object.get(stack.pop());
		if (value == null || value.isJsonNull())
			return null;
		if (!value.isJsonPrimitive())
			return null;
		if (value.getAsJsonPrimitive().isNumber())
			return Double.toString(value.getAsNumber().doubleValue());
		if (value.getAsJsonPrimitive().isBoolean())
			return value.getAsBoolean() ? "true" : "false";
		return value.getAsString();
	}

	private String[] getValues(JsonObject object, String[] fields) {
		String[] values = new String[fields.length];
		for (int i = 0; i < fields.length; i++) {
			values[i] = get(object, fields[i].split("\\."));
			if (values[i] == null)
				return null;
		}
		return values;
	}

	private boolean equal(String[] a1, String[] a2) {
		if (a1.length != a2.length)
			return false;
		for (int i = 0; i < a1.length; i++)
			if (a1[i] == a2[i])
				continue;
			else if (a1[i] == null)
				return false;
			else if (!a1[i].equals(a2[i]))
				return false;
		return true;
	}
}
