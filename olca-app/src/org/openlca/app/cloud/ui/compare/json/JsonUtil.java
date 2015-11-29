package org.openlca.app.cloud.ui.compare.json;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class JsonUtil {

	public static JsonObject toJsonObject(JsonElement element) {
		if (element == null)
			return null;
		if (!element.isJsonObject())
			return null;
		return element.getAsJsonObject();
	}

	public static JsonArray toJsonArray(JsonElement element) {
		if (element == null)
			return null;
		if (!element.isJsonArray())
			return null;
		return element.getAsJsonArray();
	}

	public static JsonPrimitive toJsonPrimitive(JsonElement element) {
		if (element == null)
			return null;
		if (!element.isJsonPrimitive())
			return null;
		return element.getAsJsonPrimitive();
	}

	public static JsonElement deepCopy(JsonElement element) {
		if (element == null)
			return null;
		if (element.isJsonPrimitive())
			return deepCopy(element.getAsJsonPrimitive());
		if (element.isJsonArray())
			return deepCopy(element.getAsJsonArray());
		if (element.isJsonObject())
			return deepCopy(element.getAsJsonObject());
		return JsonNull.INSTANCE;
	}

	private static JsonArray deepCopy(JsonArray element) {
		JsonArray copy = new JsonArray();
		element.forEach((child) -> copy.add(deepCopy(child)));
		return copy;
	}

	private static JsonObject deepCopy(JsonObject element) {
		JsonObject copy = new JsonObject();
		for (Entry<String, JsonElement> entry : element.entrySet())
			copy.add(entry.getKey(), deepCopy(entry.getValue()));
		return copy;
	}

	private static JsonPrimitive deepCopy(JsonPrimitive element) {
		if (element.isBoolean())
			return new JsonPrimitive(element.getAsBoolean());
		if (element.isNumber())
			return new JsonPrimitive(element.getAsNumber());
		return new JsonPrimitive(element.getAsString());
	}

	public static boolean equal(String property, JsonElement e1,
			JsonElement e2, ElementFinder finder) {
		if (isNull(e1) && isNull(e2))
			return true;
		if (isNull(e1) || isNull(e2))
			return false;
		if (e1.isJsonPrimitive() && e2.isJsonPrimitive())
			return equal(e1.getAsJsonPrimitive(), e2.getAsJsonPrimitive());
		if (e1.isJsonArray() && e2.isJsonArray())
			return equal(property, e1.getAsJsonArray(), e2.getAsJsonArray(),
					finder);
		if (e1.isJsonObject() && e2.isJsonObject())
			return equal(e1.getAsJsonObject(), e2.getAsJsonObject(), finder);
		return false;
	}

	private static boolean equal(String property, JsonArray a1, JsonArray a2,
			ElementFinder finder) {
		if (a1.size() != a2.size())
			return false;
		Iterator<JsonElement> it1 = a1.iterator();
		while (it1.hasNext()) {
			JsonElement e1 = it1.next();
			int index = finder.find(property, e1, a2);
			if (index == -1)
				return false;
			JsonElement e2 = a2.get(index);
			if (!equal(property, e1, e2, finder))
				return false;

		}
		return true;
	}

	private static boolean equal(JsonObject e1, JsonObject e2,
			ElementFinder finder) {
		Set<String> checked = new HashSet<>();
		for (Entry<String, JsonElement> entry : e1.entrySet()) {
			checked.add(entry.getKey());
			JsonElement element = entry.getValue();
			JsonElement other = e2.get(entry.getKey());
			if (!equal(entry.getKey(), element, other, finder))
				return false;
		}
		for (Entry<String, JsonElement> entry : e2.entrySet()) {
			if (checked.contains(entry.getKey()))
				continue;
			JsonElement element = e1.get(entry.getKey());
			JsonElement other = entry.getValue();
			if (!equal(entry.getKey(), element, other, finder))
				return false;
		}
		return true;
	}

	private static boolean equal(JsonPrimitive e1, JsonPrimitive e2) {
		if (e1.isBoolean() && e2.isBoolean())
			return e1.getAsBoolean() == e2.getAsBoolean();
		if (e1.isNumber() && e2.isNumber())
			return e1.getAsNumber().doubleValue() == e2.getAsNumber()
					.doubleValue();
		return e1.getAsString().equals(e2.getAsString());
	}

	public static boolean isNull(JsonElement element) {
		if (element == null)
			return true;
		if (element.isJsonNull())
			return true;
		if (element.isJsonArray())
			return element.getAsJsonArray().size() == 0;
		if (element.isJsonObject())
			return element.getAsJsonObject().entrySet().size() == 0;
		if (element.isJsonPrimitive())
			if (element.getAsJsonPrimitive().isNumber())
				return element.getAsJsonPrimitive().getAsNumber() == null;
			else if (element.getAsJsonPrimitive().isString())
				return element.getAsJsonPrimitive().getAsString() == null;
		return false;
	}

	public static String getString(JsonElement element, String property) {
		if (element == null)
			return null;
		if (!element.isJsonObject())
			return null;
		JsonObject object = element.getAsJsonObject();
		if (property.contains(".")) {
			String next = property.substring(0, property.indexOf('.'));
			String rest = property.substring(property.indexOf('.') + 1);
			return getString(object.get(next), rest);
		}
		if (!object.has(property))
			return null;
		return object.get(property).getAsString();
	}

	public static JsonArray replace(int index, JsonArray original,
			JsonElement toReplace) {
		JsonArray copy = new JsonArray();
		for (int i = 0; i < original.size(); i++)
			if (index == i)
				copy.add(toReplace);
			else
				copy.add(original.get(i));
		return copy;
	}

	public static JsonArray remove(int index, JsonArray original) {
		JsonArray copy = new JsonArray();
		for (int i = 0; i < original.size(); i++)
			if (index != i)
				copy.add(original.get(i));
		return copy;
	}

	public static int find(JsonElement element, JsonArray array,
			String... fields) {
		if (array == null || array.size() == 0)
			return -1;
		if (element.isJsonPrimitive())
			return findPrimitive(element.getAsJsonPrimitive(), array);
		if (fields == null)
			return -1;
		if (!element.isJsonObject())
			return -1;
		JsonObject object = element.getAsJsonObject();
		String[] values = getValues(object, fields);
		if (values == null)
			return -1;
		Iterator<JsonElement> iterator = array.iterator();
		int index = 0;
		while (iterator.hasNext()) {
			JsonElement other = iterator.next();
			if (!other.isJsonObject()) {
				index++;
				continue;
			}
			String[] otherValues = getValues(other.getAsJsonObject(), fields);
			if (equal(values, otherValues))
				return index;
			index++;
		}
		return -1;
	}

	private static int findPrimitive(JsonPrimitive element, JsonArray array) {
		Iterator<JsonElement> iterator = array.iterator();
		int index = 0;
		while (iterator.hasNext()) {
			JsonElement next = iterator.next();
			if (!next.isJsonPrimitive()) {
				index++;
				continue;
			}
			JsonPrimitive other = next.getAsJsonPrimitive();
			if (other.equals(element))
				return index;
			index++;
		}
		return -1;
	}

	private static String get(JsonObject object, String... path) {
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

	private static String[] getValues(JsonObject object, String[] fields) {
		String[] values = new String[fields.length];
		for (int i = 0; i < fields.length; i++)
			values[i] = get(object, fields[i].split("\\."));
		return values;
	}

	private static boolean equal(String[] a1, String[] a2) {
		if (a1 == null && a2 == null)
			return true;
		if (a1 == null || a2 == null)
			return false;
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

	public static abstract class ElementFinder {

		protected abstract String[] getComparisonFields(String property);

		public int find(String property, JsonElement element, JsonArray array) {
			return JsonUtil.find(element, array, getComparisonFields(property));
		}

	}

}
