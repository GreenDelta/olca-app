package org.openlca.app.cloud.ui.compare;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import org.openlca.core.model.Exchange;

import com.google.common.reflect.Parameter;
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

	public static boolean equal(JsonElement e1, JsonElement e2) {
		if (isNull(e1) && isNull(e2))
			return true;
		if (isNull(e1) || isNull(e2))
			return false;
		if (e1.isJsonPrimitive() && e2.isJsonPrimitive())
			return equal(e1.getAsJsonPrimitive(), e2.getAsJsonPrimitive());
		if (e1.isJsonArray() && e2.isJsonArray())
			return equal(e1.getAsJsonArray(), e2.getAsJsonArray());
		if (e1.isJsonObject() && e2.isJsonObject())
			return equal(e1.getAsJsonObject(), e2.getAsJsonObject());
		return false;
	}

	private static boolean isNull(JsonElement element) {
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

	private static boolean equal(JsonArray e1, JsonArray e2) {
		if (e1.size() != e2.size())
			return false;
		Iterator<JsonElement> it1 = e1.iterator();
		Iterator<JsonElement> it2 = e2.iterator();
		while (it1.hasNext())
			if (!equal(it1.next(), it2.next()))
				return false;
		return true;
	}

	private static boolean equal(JsonObject e1, JsonObject e2) {
		Set<String> checked = new HashSet<>();
		for (Entry<String, JsonElement> entry : e1.entrySet()) {
			checked.add(entry.getKey());
			if (!displayElement(entry.getKey()))
				if (!entry.getKey().equals("@id"))
					continue;
			JsonElement element = entry.getValue();
			JsonElement other = e2.get(entry.getKey());
			if (!equal(element, other))
				return false;
		}
		for (Entry<String, JsonElement> entry : e2.entrySet()) {
			if (checked.contains(entry.getKey()))
				continue;
			if (!displayElement(entry.getKey()))
				if (!entry.getKey().equals("@id"))
					continue;
			JsonElement element = e1.get(entry.getKey());
			JsonElement other = entry.getValue();
			if (!equal(element, other))
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

	static boolean isReference(JsonElement element) {
		if (element == null)
			return false;
		if (!element.isJsonObject())
			return false;
		JsonObject object = element.getAsJsonObject();
		if (object.get("@id") == null)
			return false;
		JsonElement type = object.get("@type");
		if (type == null)
			return false;
		if (type.getAsString().equals(Parameter.class.getSimpleName()))
			if (!isGlobalParameter(object))
				return false;
		if (type.getAsString().equals(Exchange.class.getSimpleName()))
			return false;
		return true;
	}

	private static boolean isGlobalParameter(JsonObject parameter) {
		JsonElement scope = parameter.get("parameterScope");
		if (scope == null)
			return false;
		return "GLOBAL_SCOPE".equals(scope.getAsString());
	}

	static boolean displayElement(String key) {
		if (key.startsWith("@"))
			return false;
		if (key.equals("lastChange"))
			return false;
		if (key.equals("version"))
			return false;
		if (key.equals("parameterScope"))
			return false;
		return true;
	}

	public static String getType(JsonElement element) {
		if (element == null)
			return null;
		if (!element.isJsonObject())
			return null;
		JsonElement type = element.getAsJsonObject().get("@type");
		if (type == null)
			return null;
		return type.getAsString();
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

	static JsonArray deepCopy(JsonArray element) {
		JsonArray copy = new JsonArray();
		element.forEach((child) -> copy.add(deepCopy(child)));
		return copy;
	}

	static JsonObject deepCopy(JsonObject element) {
		JsonObject copy = new JsonObject();
		for (Entry<String, JsonElement> entry : element.entrySet())
			copy.add(entry.getKey(), deepCopy(entry.getValue()));
		return copy;
	}

	static JsonPrimitive deepCopy(JsonPrimitive element) {
		if (element.isBoolean())
			return new JsonPrimitive(element.getAsBoolean());
		if (element.isNumber())
			return new JsonPrimitive(element.getAsNumber());
		return new JsonPrimitive(element.getAsString());
	}

	static int find(String key, JsonElement element, JsonArray array) {
		if (element == null)
			return -1;
		if (key.equals("units"))
			return find(element, array, "name");
		if (key.equals("flowProperties"))
			return find(element, array, "flowProperty.@id");
		if (key.equals("exchanges"))
			return find(element, array, "flow.@id", "input");
		return find(element, array, "@id");
	}

	private static int find(JsonElement element, JsonArray array,
			String... fields) {
		if (fields == null)
			return -1;
		JsonObject object = element.getAsJsonObject();
		String[] values = getValues(object, fields);
		if (values == null)
			return -1;
		if (array == null)
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
		for (int i = 0; i < fields.length; i++) {
			values[i] = get(object, fields[i].split("\\."));
			if (values[i] == null)
				return null;
		}
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

	static JsonArray replace(int index, JsonArray original,
			JsonElement toReplace) {
		JsonArray copy = new JsonArray();
		for (int i = 0; i < original.size(); i++)
			if (index == i)
				copy.add(toReplace);
			else
				copy.add(original.get(i));
		return copy;
	}

	static JsonArray remove(int index, JsonArray original) {
		JsonArray copy = new JsonArray();
		for (int i = 0; i < original.size(); i++)
			if (index != i)
				copy.add(original.get(i));
		return copy;
	}

}
