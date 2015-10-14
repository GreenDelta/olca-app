package org.openlca.app.cloud.ui;

import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonToText {

	private final static String TAB = "  ";

	public static String toText(JsonObject json) {
		return toText(json, 0);
	}

	public static String toText(JsonObject json, int indentation) {
		String text = "";
		for (Entry<String, JsonElement> entry : json.entrySet()) {
			if (!displayElement(entry.getKey()))
				continue;
			text += getTabs(indentation) + entry.getKey() + ": "
					+ toText(entry.getValue(), indentation);
			text += "\n";
		}
		if (text.endsWith("\n"))
			text = text.substring(0, text.lastIndexOf("\n"));
		return text;
	}

	public static String toText(JsonElement element, int indentation) {
		if (element == null || element.isJsonNull())
			return "";
		if (element.isJsonPrimitive())
			return element.getAsString();
		if (element.isJsonArray()) {
			String text = "[\n";
			JsonArray array = element.getAsJsonArray();
			for (int i = 0; i < array.size(); i++)
				text += getTabs(indentation + 1) + i + ": "
						+ toText(array.get(i), indentation + 1) + "\n";
			text += getTabs(indentation) + "]";
			return text;
		}
		if (element.isJsonObject()) {
			JsonObject object = element.getAsJsonObject();
			if (isReference(object))
				return object.get("name").getAsString();
			else if (object.entrySet().size() != 0)
				return "\n" + toText(object, indentation + 1);
			return "";
		}
		return null;
	}

	private static String getTabs(int indentation) {
		String text = "";
		for (int i = 0; i < indentation; i++)
			text += TAB;
		return text;
	}

	private static boolean isReference(JsonObject object) {
		return object.get("@id") != null;
	}

	private static boolean displayElement(String key) {
		if (key.startsWith("@"))
			return false;
		if (key.equals("lastChange"))
			return false;
		if (key.equals("version"))
			return false;
		return true;
	}

}
