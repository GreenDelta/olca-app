package org.openlca.app.tools.mcp.tools;

import java.util.Map;
import java.util.Optional;

import org.openlca.commons.Strings;
import org.openlca.core.model.ModelType;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/// Typed access to the arguments of a tool call. The MCP SDK passes the
/// arguments as a generic map; this class converts them into the types we need,
/// including Gson objects for the openLCA JSON-LD services.
public record ToolArgs(Map<String, Object> map) {

	private static final Gson gson = new Gson();

	public ToolArgs {
		map = map != null ? map : Map.of();
	}

	public Optional<String> string(String key) {
		var val = map.get(key);
		if (val == null)
			return Optional.empty();
		var s = val.toString().strip();
		return Strings.isBlank(s)
				? Optional.empty()
				: Optional.of(s);
	}

	public int integer(String key, int defaultValue) {
		var val = map.get(key);
		if (val instanceof Number num)
			return num.intValue();
		if (val instanceof String s) {
			try {
				return Integer.parseInt(s.strip());
			} catch (NumberFormatException _) {
				return defaultValue;
			}
		}
		return defaultValue;
	}

	public Optional<Double> number(String key) {
		var val = map.get(key);
		if (val instanceof Number num)
			return Optional.of(num.doubleValue());
		if (val instanceof String s) {
			try {
				return Optional.of(Double.parseDouble(s.strip()));
			} catch (NumberFormatException _) {
				return Optional.empty();
			}
		}
		return Optional.empty();
	}

	public boolean bool(String key, boolean defaultValue) {
		var val = map.get(key);
		if (val instanceof Boolean b)
			return b;
		if (val instanceof String s)
			return Boolean.parseBoolean(s.strip());
		return defaultValue;
	}

	/// Returns the argument as a Gson object. Agents sometimes pass JSON
	/// objects as strings, so we also try to parse string values.
	public Optional<JsonObject> json(String key) {
		var val = map.get(key);
		if (val == null)
			return Optional.empty();
		try {
			if (val instanceof String s) {
				var elem = JsonParser.parseString(s);
				return elem.isJsonObject()
						? Optional.of(elem.getAsJsonObject())
						: Optional.empty();
			}
			var elem = gson.toJsonTree(val);
			return elem.isJsonObject()
					? Optional.of(elem.getAsJsonObject())
					: Optional.empty();
		} catch (Exception _) {
			return Optional.empty();
		}
	}

	/// Returns the model type of the given argument. The type is given by the
	/// name of the respective JSON-LD type, e.g. `Process` or `Flow`.
	public Optional<ModelType> modelType(String key) {
		return string(key).map(ToolArgs::modelTypeOf);
	}

	static ModelType modelTypeOf(String name) {
		if (name == null)
			return null;
		for (var type : ModelType.values()) {
			var typeName = typeNameOf(type);
			if (typeName.equalsIgnoreCase(name)
					|| type.name().equalsIgnoreCase(name))
				return type;
		}
		return null;
	}

	/// Returns the JSON-LD type name of the given model type, e.g. `Process`.
	static String typeNameOf(ModelType type) {
		return type.getModelClass().getSimpleName();
	}
}
