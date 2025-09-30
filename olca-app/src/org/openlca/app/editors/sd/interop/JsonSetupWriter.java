package org.openlca.app.editors.sd.interop;

import java.io.File;

import org.openlca.core.model.ModelType;
import org.openlca.core.model.ParameterRedef;
import org.openlca.jsonld.Json;
import org.openlca.util.Res;
import org.openlca.util.Strings;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class JsonSetupWriter {

	private final SimulationSetup setup;

	private JsonSetupWriter(SimulationSetup setup) {
		this.setup = setup;
	}

	public static Res<Void> write(SimulationSetup setup, File file) {
		if (setup == null)
			return Res.error("no simulation setup provided");
		if (file == null)
			return Res.error("no file provided");
		try {
			var writer = new JsonSetupWriter(setup);
			var json = writer.write();
			Json.write(json, file);
			return Res.VOID;
		} catch (Exception e) {
			return Res.error("failed to write setup", e);
		}
	}

	private JsonObject write() {
		var json = new JsonObject();
		var method = setup.method();
		if (method != null) {
			Json.put(json, "impactMethod", Json.asRef(method));
		}

		writeSystemBindings(json);
		return json;
	}

	private void writeSystemBindings(JsonObject json) {
		if (setup.systemBindings().isEmpty())
			return;
		var array = new JsonArray();
		for (var binding : setup.systemBindings()) {
			var obj = systemBindingOf(binding);
			if (obj != null) {
				array.add(obj);
			}
		}
		json.add("systemBindings", array);
	}

	private JsonObject systemBindingOf(SystemBinding binding) {
		if (binding == null || binding.system() == null)
			return null;

		var obj = new JsonObject();
		Json.put(obj, "system", Json.asRef(binding.system()));
		Json.put(obj, "amount", binding.amount());
		Json.put(obj, "allocation", binding.allocation());

		if (!binding.varBindings().isEmpty()) {
			var array = new JsonArray();
			for (var varBinding : binding.varBindings()) {
				var vbObj = varBindingOf(varBinding);
				if (vbObj != null) {
					array.add(vbObj);
				}
			}
			obj.add("varBindings", array);
		}

		return obj;
	}

	private JsonObject varBindingOf(VarBinding varBinding) {
		if (varBinding == null
				|| varBinding.varId() == null
				|| varBinding.parameter() == null)
			return null;

		var obj = new JsonObject();
		Json.put(obj, "var", varBinding.varId().label());
		var paramObj = paramObjOf(varBinding.parameter());
		if (paramObj != null) {
			obj.add("parameter", paramObj);
		}
		return obj;
	}

	private JsonObject paramObjOf(ParameterRedef param) {
		if (param == null || Strings.nullOrEmpty(param.name))
			return null;

		var obj = new JsonObject();
		Json.put(obj, "name", param.name);
		Json.put(obj, "value", param.value);
		Json.put(obj, "description", param.description);

		if (param.contextId != null && param.contextType != null) {
			var contextObj = new JsonObject();
			Json.put(contextObj, "@id", param.contextId.toString());

			var type = param.contextType == ModelType.IMPACT_CATEGORY
					? "ImpactCategory"
					: "Process";
			Json.put(contextObj, "@type", type);
			obj.add("context", contextObj);
		}
		return obj;
	}
}
