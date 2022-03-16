package org.openlca.app.collaboration.viewers.diff;

import org.eclipse.jgit.lib.ObjectId;
import org.openlca.app.collaboration.util.Json;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.core.database.Daos;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.Result;
import org.openlca.core.model.RootEntity;
import org.openlca.git.model.Reference;
import org.openlca.jsonld.output.JsonExport;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

class RefJson {

	private static Gson gson = new Gson();

	static JsonObject get(Reference ref) {
		if (ref.type == null || ref.refId == null)
			return null;
		if (ref.objectId == null || ref.objectId.equals(ObjectId.zeroId()))
			return getLocalJson(ref);
		var json = gson.fromJson(Repository.get().datasets.get(ref.objectId), JsonObject.class);
		if (json == null)
			return null;
		var type = Json.getString(json, "@type");
		if (Process.class.getSimpleName().equals(type)) {
			split(json, "exchanges", "input", "inputs", "outputs");
		} else if (Result.class.getSimpleName().equals(type)) {
			split(json, "flowResults", "isInput", "inputResults", "outputResults");
		}
		return json;
	}

	private static JsonObject getLocalJson(Reference ref) {
		var entity = load(ref.type, ref.refId);
		if (entity == null)
			return null;
		var json = JsonExport.toJson(entity, Database.get());
		if (entity instanceof Process) {
			split(json, "exchanges", "input", "inputs", "outputs");
		} else if (entity instanceof Result) {
			split(json, "flowResults", "isInput", "inputResults", "outputResults");
		}
		return json;
	}

	private static RootEntity load(ModelType type, String refId) {
		if (type == null || refId == null)
			return null;
		return Daos.root(Database.get(), type).getForRefId(refId);
	}

	private static void split(JsonObject obj, String arrayProperty, String splitProperty, String property1,
			String property2) {
		var exchanges = obj.getAsJsonArray(arrayProperty);
		var inputs = new JsonArray();
		var outputs = new JsonArray();
		if (exchanges != null) {
			for (var elem : exchanges) {
				var e = elem.getAsJsonObject();
				var isInput = e.get(splitProperty);
				if (isInput.isJsonPrimitive() && isInput.getAsBoolean()) {
					inputs.add(e);
				} else {
					outputs.add(e);
				}
			}
		}
		obj.remove(arrayProperty);
		obj.add(property1, inputs);
		obj.add(property2, outputs);
	}

}
