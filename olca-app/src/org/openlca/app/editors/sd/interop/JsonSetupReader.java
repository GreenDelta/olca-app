package org.openlca.app.editors.sd.interop;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.openlca.commons.Res;
import org.openlca.commons.Strings;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.jsonld.Json;
import org.openlca.sd.model.Id;
import org.openlca.sd.interop.Rect;
import org.openlca.sd.interop.SimulationSetup;
import org.openlca.sd.interop.SystemBinding;
import org.openlca.sd.interop.VarBinding;

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
			readPositions(setup);
			return Res.ok(setup);
		} catch (Exception e) {
			return Res.error("Failed to parse setup", e);
		}
	}

	private ImpactMethod readImpactMethod() {
		var refId = Json.getRefId(json, "impactMethod");
		if (Strings.isBlank(refId))
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
			var system = systemOf(obj);
			if (system == null)
				return null;
			var binding = new SystemBinding()
					.system(system)
					.amount(Json.getDouble(obj, "amount", 1.0));

			var allocation = Json.getEnum(obj, "allocation", AllocationMethod.class);
			binding.allocation(allocation != null
					? allocation
					: AllocationMethod.USE_DEFAULT);

			Json.forEachObject(obj, "varBindings", o -> {
				var vb = varBindingOf(o);
				if (vb != null) {
					binding.varBindings().add(vb);
				}
			});
			return binding;
		} catch (Exception e) {
			return null;
		}
	}

	private ProductSystem systemOf(JsonObject obj) {
		var refId = Json.getRefId(obj, "system");
		return Strings.isNotBlank(refId)
				? db.get(ProductSystem.class, refId)
				: null;
	}

	private VarBinding varBindingOf(JsonObject obj) {
		var varId = Id.of(Json.getString(obj, "var"));
		if (varId.isNil())
			return null;
		var param = parameterOf(obj);
		if (param == null)
			return null;
		return new VarBinding()
				.varId(varId)
				.parameter(param);
	}

	private ParameterRedef parameterOf(JsonObject obj) {
		var paramObj = Json.getObject(obj, "parameter");
		if (paramObj == null)
			return null;
		var name = Json.getString(paramObj, "name");
		if (Strings.isBlank(name))
			return null;
		var redef = new ParameterRedef();
		redef.name = name;
		redef.value = Json.getDouble(paramObj, "value", 0);
		redef.description = Json.getString(paramObj, "description");

		var contextObj = Json.getObject(paramObj, "context");
		if (contextObj == null)
			return redef;
		var type = Json.getString(contextObj, "@type");
		var refId = Json.getString(contextObj, "@id");
		if (Strings.isBlank(refId))
			return redef;
		var d = "ImpactCategory".equals(type)
				? db.getDescriptor(ImpactCategory.class, refId)
				: db.getDescriptor(Process.class, refId);
		if (d != null) {
			redef.contextId = d.id;
			redef.contextType = d.type;
		}
		return redef;
	}

	private void readPositions(SimulationSetup setup) {
		var obj = Json.getObject(json, "positions");
		if (obj == null)
			return;
		for (var entry : obj.entrySet()) {
			var id = Id.of(entry.getKey());
			if (id.isNil() || !entry.getValue().isJsonObject())
				continue;
			var o = entry.getValue().getAsJsonObject();
			var rect = new Rect(
					Json.getInt(o, "x", 0),
					Json.getInt(o, "y", 0),
					Json.getInt(o, "width", 0),
					Json.getInt(o, "height", 0));
			setup.positions().put(id, rect);
		}
	}
}
