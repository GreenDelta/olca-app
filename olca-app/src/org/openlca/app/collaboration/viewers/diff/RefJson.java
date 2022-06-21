package org.openlca.app.collaboration.viewers.diff;

import org.eclipse.jgit.lib.ObjectId;
import org.openlca.app.collaboration.util.Json;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.core.database.Daos;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.RootEntity;
import org.openlca.jsonld.output.JsonExport;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

class RefJson {

	private static Gson gson = new Gson();

	static JsonObject get(ModelType type, String refId, ObjectId objectId) {
		if (type == null || refId == null)
			return null;
		if (objectId == null || objectId.equals(ObjectId.zeroId()))
			return getLocalJson(type, refId);
		var json = gson.fromJson(Repository.get().datasets.get(objectId), JsonObject.class);
		if (json == null)
			return null;
		split(json, type);
		return json;
	}

	private static JsonObject getLocalJson(ModelType type, String refId) {
		var entity = load(type, refId);
		if (entity == null)
			return null;
		var json = JsonExport.toJson(entity, Database.get());
		split(json, type);
		return json;
	}

	private static RootEntity load(ModelType type, String refId) {
		if (type == null || refId == null)
			return null;
		return Daos.root(Database.get(), type).getForRefId(refId);
	}

	private static void split(JsonObject json, ModelType type) {
		if (type == ModelType.PROCESS) {
			split(json, "exchanges", "isInput", "inputs", "outputs");
		} else if (type == ModelType.RESULT) {
			split(json, "flowResults", "isInput", "inputResults", "outputResults");
		}
	}

	private static void split(JsonObject json, String arrayProperty, String splitProperty,
			String property1, String property2) {
		var exchanges = json.getAsJsonArray(arrayProperty);
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
		json.remove(arrayProperty);
		json.add(property1, inputs);
		json.add(property2, outputs);
	}

	static void joinSplitFields(JsonObject json) {
		var type = Json.getModelType(json);
		if (type == ModelType.PROCESS) {
			join(json, "exchanges", "inputs", "outputs");
		} else if (type == ModelType.RESULT) {
			join(json, "flowResults", "inputResults", "outputResults");
		}
	}

	private static void join(JsonObject json, String arrayProperty, String... joinProperties) {
		var joined = new JsonArray();
		for (var joinProperty : joinProperties) {
			var toJoin = json.getAsJsonArray(joinProperty);
			joined.addAll(toJoin);
		}
		json.add(arrayProperty, joined);
	}

}
