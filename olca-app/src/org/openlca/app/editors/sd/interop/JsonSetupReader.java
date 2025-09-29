package org.openlca.app.editors.sd.interop;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ProductSystem;
import org.openlca.jsonld.Json;
import org.openlca.sd.eqn.Id;
import org.openlca.util.Res;
import org.openlca.util.Strings;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonSetupReader {

	private final JsonObject json;
	private final IDatabase db;

	private JsonSetupReader(JsonObject json, IDatabase db) {
		this.json = json;
		this.db = db;
	}

	public static Res<SimulationSetup> read(File file, IDatabase db) {
		if (file == null || !file.exists())
			return Res.error("no setup file provided");
		if (db == null)
			return Res.error("no database provided");
		var json = Json.readObject(file).orElse(null);
		if (json == null)
			return Res.error("failed to read JSON file");
		return new JsonSetupReader(json, db).read();
	}

	private Res<SimulationSetup> read() {
		try {
			var setup = new SimulationSetup();
			setup.method(readImpactMethod());
			for (var sb : readSystemBindings()) {
				setup.systemBindings().add(sb);
			}
			return Res.of(setup);
		} catch (Exception e) {
			return Res.error("Failed to parse setup", e);
		}
	}

	private ImpactMethod readImpactMethod() {
		var refId = Json.getRefId(json, "impactMethod");
		if (Strings.nullOrEmpty(refId))
			return null;
		return db.get(ImpactMethod.class, refId);
	}

	private List<SystemBinding> readSystemBindings() {
		var array = Json.getArray(json, "systemBindings");
		if (array == null)
			return List.of();
		var bindings = new ArrayList<SystemBinding>();
		for (var e : array) {
			if (!e.isJsonObject())
				continue;
			var b = systemBindingOf(e.getAsJsonObject());
			if (b != null) {
				bindings.add(b);
			}
		}
		return bindings;
	}

	private SystemBinding systemBindingOf(JsonObject obj) {
		try {
			var binding = new SystemBinding();

			// Read system
			readProductSystem(obj).ifPresent(binding::system);

			// Read quantitative reference
			readAmount(obj).ifPresent(binding::amount);
			// TODO: Read unit, property, flow

			// Read variable bindings
			readVarBindings(obj).forEach(binding.varBindings()::add);

			return java.util.Optional.of(binding);
		} catch (Exception e) {
			// Log error if needed
			return java.util.Optional.empty();
		}
	}

	private java.util.Optional<ProductSystem> readProductSystem(JsonObject obj) {
		var systemObj = obj.get("system");
		if (systemObj == null || !systemObj.isJsonObject()) {
			return java.util.Optional.empty();
		}

		// TODO: Map system by ID or name
		// var id = systemObj.getAsJsonObject().get("id");
		// var name = systemObj.getAsJsonObject().get("name");
		// return findProductSystem(id, name);

		return java.util.Optional.empty();
	}

	private java.util.Optional<Double> readAmount(JsonObject obj) {
		var amountElement = obj.get("amount");
		if (amountElement != null && amountElement.isJsonPrimitive()) {
			try {
				return java.util.Optional.of(amountElement.getAsDouble());
			} catch (Exception ignored) {
			}
		}
		return java.util.Optional.empty();
	}

	private java.util.List<VarBinding> readVarBindings(JsonObject obj) {
		var bindings = new ArrayList<VarBinding>();
		var bindingsArray = obj.get("varBindings");

		if (bindingsArray == null || !bindingsArray.isJsonArray()) {
			return bindings;
		}

		for (JsonElement element : bindingsArray.getAsJsonArray()) {
			if (element.isJsonObject()) {
				readVarBinding(element.getAsJsonObject())
					.ifPresent(bindings::add);
			}
		}

		return bindings;
	}

	private java.util.Optional<VarBinding> readVarBinding(JsonObject obj) {
		try {
			var binding = new VarBinding();

			// Read model variable
			readModelVariable(obj).ifPresent(binding::varId);

			// Read parameter
			readParameter(obj).ifPresent(binding::parameter);

			return java.util.Optional.of(binding);
		} catch (Exception e) {
			return java.util.Optional.empty();
		}
	}

	private java.util.Optional<Id> readModelVariable(JsonObject obj) {
		var varObj = obj.get("modelVariable");
		if (varObj == null || !varObj.isJsonObject()) {
			return java.util.Optional.empty();
		}

		var name = varObj.getAsJsonObject().get("name");
		if (name != null && name.isJsonPrimitive()) {
			var varName = name.getAsString();
			if (Strings.notEmpty(varName)) {
				// TODO: Create proper Id instance
				// return Optional.of(Id.of(varName));
			}
		}

		return java.util.Optional.empty();
	}

	private java.util.Optional<Parameter> readParameter(JsonObject obj) {
		var paramObj = obj.get("parameter");
		if (paramObj == null || !paramObj.isJsonObject()) {
			return java.util.Optional.empty();
		}

		// TODO: Map parameter by ID or name
		// var id = paramObj.getAsJsonObject().get("id");
		// var name = paramObj.getAsJsonObject().get("name");
		// return findParameter(id, name);

		return java.util.Optional.empty();
	}

	// TODO: Implement database lookup methods
	// private Optional<ImpactMethod> findImpactMethod(JsonElement id, JsonElement name) { ... }
	// private Optional<ProductSystem> findProductSystem(JsonElement id, JsonElement name) { ... }
	// private Optional<Parameter> findParameter(JsonElement id, JsonElement name) { ... }
}
