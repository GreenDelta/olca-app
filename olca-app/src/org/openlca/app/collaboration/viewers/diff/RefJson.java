package org.openlca.app.collaboration.viewers.diff;

import java.time.Instant;

import org.eclipse.jgit.lib.ObjectId;
import org.openlca.app.collaboration.util.Json;
import org.openlca.app.collaboration.viewers.json.content.JsonNode;
import org.openlca.app.db.Database;
import org.openlca.core.database.Daos;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.Version;
import org.openlca.git.model.ModelRef;
import org.openlca.git.model.Reference;
import org.openlca.git.repo.OlcaRepository;
import org.openlca.jsonld.output.JsonExport;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class RefJson {

	private static Gson gson = new Gson();

	public static JsonObject get(OlcaRepository repo, Reference remote) {
		return get(repo, remote, null);
	}

	static JsonObject get(OlcaRepository repo, Reference remote, ModelRef local) {
		if ((remote != null && remote.isCategory) || (local != null && local.isCategory))
			return null;
		if (remote == null || remote.objectId.equals(ObjectId.zeroId())) {
			if (local == null)
				return null;
			return getLocalJson(local.type, local.refId);
		}
		var json = gson.fromJson(repo.datasets.get(remote), JsonObject.class);
		if (json == null)
			return null;
		split(json, remote.type);
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
		var array = json.getAsJsonArray(arrayProperty);
		var values1 = new JsonArray();
		var values2 = new JsonArray();
		if (array != null) {
			for (var elem : array) {
				var e = elem.getAsJsonObject();
				var isInput = e.get(splitProperty);
				if (isInput.isJsonPrimitive() && isInput.getAsBoolean()) {
					values1.add(e);
				} else {
					values2.add(e);
				}
			}
		}
		json.remove(arrayProperty);
		json.add(property1, values1);
		json.add(property2, values2);
	}

	static JsonObject getMergedData(JsonNode node) {
		var merged = node.left.getAsJsonObject();
		var remote = node.right.getAsJsonObject();
		var type = Json.getModelType(merged);
		joinSplitFields(merged, type);
		if (type == ModelType.PROCESS) {
			correctLastInternalId(merged, remote);
		}
		updateVersion(merged, remote);
		return merged;
	}

	private static void joinSplitFields(JsonObject json, ModelType type) {
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

	private static void updateVersion(JsonObject merged, JsonObject remote) {
		var version = Version.fromString(remote.get("version").getAsString());
		version.incUpdate();
		merged.addProperty("version", Version.asString(version.getValue()));
		merged.addProperty("lastChange", Instant.now().toString());
	}

	private static void correctLastInternalId(JsonObject merged, JsonObject remote) {
		var mergedLastId = Json.getInt(merged, "lastInternalId", 0);
		var remoteLastId = Json.getInt(remote, "lastInternalId", 0);
		if (remoteLastId > mergedLastId) {
			merged.addProperty("lastInternalId", remoteLastId);
		}
	}

}
