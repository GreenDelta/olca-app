package org.openlca.app.collaboration.viewers.diff;

import org.openlca.app.collaboration.util.Json;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.core.database.Daos;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
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
		if (ref.objectId == null)
			return getLocalJson(ref);
		var json = gson.fromJson(Repository.get().datasets.get(ref.objectId), JsonObject.class);
		if (json == null)
			return null;
		var type = Json.getString(json, "@type");
		if (Process.class.getSimpleName().equals(type)) {
			splitExchanges(json);
		}
		return json;
	}

	private static JsonObject getLocalJson(Reference ref) {
		var entity = load(ref.type, ref.refId);
		if (entity == null)
			return null;
		var json = JsonExport.toJson(entity, Database.get());
		if (entity instanceof Process) {
			splitExchanges(json);
		}
		return json;
	}

	private static CategorizedEntity load(ModelType type, String refId) {
		if (type == null || refId == null)
			return null;
		return Daos.categorized(Database.get(), type).getForRefId(refId);
	}

	private static void splitExchanges(JsonObject obj) {
		var exchanges = obj.getAsJsonArray("exchanges");
		var inputs = new JsonArray();
		var outputs = new JsonArray();
		if (exchanges != null) {
			for (var elem : exchanges) {
				var e = elem.getAsJsonObject();
				var isInput = e.get("input");
				if (isInput.isJsonPrimitive() && isInput.getAsBoolean()) {
					inputs.add(e);
				} else {
					outputs.add(e);
				}
			}
		}
		obj.remove("exchanges");
		obj.add("inputs", inputs);
		obj.add("outputs", outputs);
	}

}
