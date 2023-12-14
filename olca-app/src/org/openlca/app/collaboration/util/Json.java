package org.openlca.app.collaboration.util;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import org.openlca.core.model.ModelType;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;

public class Json {

	private static final Gson gson = new Gson();
	private static final TypeAdapter<JsonElement> strictAdapter = gson.getAdapter(JsonElement.class);

	public static boolean isValid(String json) {
	    try {
	        strictAdapter.fromJson(json);
	    } catch (JsonSyntaxException | IOException e) {
	        return false;
	    }
	    return true;
	}

	public static JsonElement parse(String json) {
		return gson.fromJson(json, JsonElement.class);
	}

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
		var copy = new JsonArray();
		element.forEach((child) -> copy.add(deepCopy(child)));
		return copy;
	}

	private static JsonObject deepCopy(JsonObject element) {
		var copy = new JsonObject();
		for (Entry<String, JsonElement> entry : element.entrySet()) {
			copy.add(entry.getKey(), deepCopy(entry.getValue()));
		}
		return copy;
	}

	private static JsonPrimitive deepCopy(JsonPrimitive element) {
		if (element.isBoolean())
			return new JsonPrimitive(element.getAsBoolean());
		if (element.isNumber())
			return new JsonPrimitive(element.getAsNumber());
		return new JsonPrimitive(element.getAsString());
	}

	public static boolean equal(String property, JsonElement e1, JsonElement e2, ElementFinder finder) {
		if (isNull(e1) && isNull(e2))
			return true;
		if (isNull(e1) || isNull(e2))
			return false;
		if (e1.isJsonPrimitive() && e2.isJsonPrimitive())
			return equal(e1.getAsJsonPrimitive(), e2.getAsJsonPrimitive());
		if (e1.isJsonArray() && e2.isJsonArray())
			return equal(property, e1.getAsJsonArray(), e2.getAsJsonArray(), finder);
		if (e1.isJsonObject() && e2.isJsonObject())
			return equal(property, e1.getAsJsonObject(), e2.getAsJsonObject(), finder);
		return false;
	}

	private static boolean equal(String property, JsonArray a1, JsonArray a2, ElementFinder finder) {
		if (a1.size() != a2.size())
			return false;
		var it1 = a1.iterator();
		var used = new HashSet<Integer>();
		while (it1.hasNext()) {
			var e1 = it1.next();
			var index = finder.find(property, e1, a2, used);
			if (index == -1)
				return false;
			var e2 = a2.get(index);
			if (!equal(property, e1, e2, finder))
				return false;
			used.add(index);
		}
		return true;
	}

	private static boolean equal(String property, JsonObject e1, JsonObject e2, ElementFinder finder) {
		var checked = new HashSet<String>();
		for (var entry : e1.entrySet()) {
			checked.add(entry.getKey());
			if (finder.skipOnEqualsCheck(property, e1, entry.getKey()))
				continue;
			JsonElement element = entry.getValue();
			JsonElement other = e2.get(entry.getKey());
			if (!equal(entry.getKey(), element, other, finder))
				return false;
		}
		for (var entry : e2.entrySet()) {
			if (checked.contains(entry.getKey()))
				continue;
			if (finder.skipOnEqualsCheck(property, e1, entry.getKey()))
				continue;
			var element = e1.get(entry.getKey());
			var other = entry.getValue();
			if (!equal(entry.getKey(), element, other, finder))
				return false;
		}
		return true;
	}

	private static boolean equal(JsonPrimitive e1, JsonPrimitive e2) {
		if (e1.isBoolean() && e2.isBoolean())
			return e1.getAsBoolean() == e2.getAsBoolean();
		if (e1.isNumber() && e2.isNumber())
			return e1.getAsNumber().doubleValue() == e2.getAsNumber().doubleValue();
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
		var value = getValue(element, property);
		if (value == null || !value.isJsonPrimitive())
			return null;
		var prim = value.getAsJsonPrimitive();
		return prim.isString()
				? prim.getAsString()
				: null;
	}

	public static double getDouble(JsonElement element, String property) {
		return getDouble(element, property, 0d);
	}

	public static Double getDouble(JsonElement element, String property, Double defaultValue) {
		var value = getValue(element, property);
		if (!value.isJsonPrimitive())
			return defaultValue;
		var primitive = value.getAsJsonPrimitive();
		if (primitive.isNumber())
			return primitive.getAsDouble();
		if (!primitive.isString())
			return defaultValue;
		try {
			return Double.parseDouble(primitive.getAsString());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public static Integer getInt(JsonElement element, String property, Integer defaultValue) {
		var value = getValue(element, property);
		if (!value.isJsonPrimitive())
			return defaultValue;
		var primitive = value.getAsJsonPrimitive();
		if (primitive.isNumber())
			return primitive.getAsInt();
		if (!primitive.isString())
			return defaultValue;
		try {
			return Integer.parseInt(primitive.getAsString());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public static Long getLong(JsonElement element, String property, Long defaultValue) {
		var value = getValue(element, property);
		if (!value.isJsonPrimitive())
			return defaultValue;
		var primitive = value.getAsJsonPrimitive();
		if (primitive.isNumber())
			return primitive.getAsLong();
		if (!primitive.isString())
			return defaultValue;
		try {
			return Long.parseLong(primitive.getAsString());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public static Boolean getBoolean(JsonElement element, String property, Boolean defaultValue) {
		var value = getValue(element, property);
		if (!value.isJsonPrimitive())
			return defaultValue;
		var primitive = value.getAsJsonPrimitive();
		if (primitive.isBoolean())
			return primitive.getAsBoolean();
		if (primitive.isNumber())
			return primitive.getAsDouble() == 1;
		if (primitive.isString())
			return Boolean.parseBoolean(primitive.getAsString());
		return defaultValue;
	}

	public static JsonElement getValue(JsonElement element, String property) {
		if (element == null)
			return null;
		if (!element.isJsonObject())
			return null;
		var object = element.getAsJsonObject();
		if (property.contains(".")) {
			var next = property.substring(0, property.indexOf('.'));
			var rest = property.substring(property.indexOf('.') + 1);
			return getValue(object.get(next), rest);
		}
		if (!object.has(property))
			return null;
		return object.get(property);
	}

	public static JsonArray replace(int index, JsonArray original, JsonElement toReplace) {
		var copy = new JsonArray();
		for (var i = 0; i < original.size(); i++) {
			var toAdd = index == i
					? toReplace
					: original.get(i);
			copy.add(toAdd);
		}
		return copy;

	}

	public static JsonArray remove(int index, JsonArray original) {
		var copy = new JsonArray();
		for (var i = 0; i < original.size(); i++) {
			if (index != i) {
				copy.add(original.get(i));
			}
		}
		return copy;
	}

	public static int find(JsonElement element, JsonArray array, Set<Integer> exclude, String... fields) {
		if (array == null || array.size() == 0)
			return -1;
		if (element == null)
			return -1;
		if (element.isJsonPrimitive())
			return findPrimitive(element.getAsJsonPrimitive(), array);
		if (fields == null)
			return -1;
		if (!element.isJsonObject())
			return -1;
		var object = element.getAsJsonObject();
		var values = getValues(object, fields);
		if (values == null)
			return -1;
		var iterator = array.iterator();
		var index = 0;
		while (iterator.hasNext()) {
			var other = iterator.next();
			if (!other.isJsonObject()) {
				index++;
				continue;
			}
			var otherValues = getValues(other.getAsJsonObject(), fields);
			if (equal(values, otherValues) && (exclude == null || !exclude.contains(index)))
				return index;
			index++;
		}
		return -1;
	}

	public static ModelType getModelType(JsonElement element) {
		var type = getValue(element, "@type");
		if (type == null)
			return null;
		for (var mType : ModelType.values())
			if (mType.getModelClass().getSimpleName().equals(type.getAsString()))
				return mType;
		return null;
	}

	public static String getName(JsonElement element) {
		if (element == null)
			return null;
		if (!element.isJsonObject())
			return null;
		var name = element.getAsJsonObject().get("name");
		if (name == null)
			return null;
		return name.getAsString();
	}

	public static boolean isJsonObject(JsonElement element) {
		return element != null && element.isJsonObject();
	}

	private static int findPrimitive(JsonPrimitive element, JsonArray array) {
		var iterator = array.iterator();
		var index = 0;
		while (iterator.hasNext()) {
			var next = iterator.next();
			if (!next.isJsonPrimitive()) {
				index++;
				continue;
			}
			var other = next.getAsJsonPrimitive();
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
		var stack = new Stack<String>();
		for (int i = path.length - 1; i >= 0; i--)
			stack.add(path[i]);
		while (stack.size() > 1) {
			var next = stack.pop();
			if (!object.has(next))
				return null;
			object = object.get(next).getAsJsonObject();
		}
		var value = object.get(stack.pop());
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
		var values = new String[fields.length];
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

		protected abstract boolean skipOnEqualsCheck(String parentProperty, JsonElement element, String property);

		public int find(String property, JsonElement element, JsonArray array, Set<Integer> exclude) {
			return Json.find(element, array, exclude, getComparisonFields(property));
		}

	}

}
