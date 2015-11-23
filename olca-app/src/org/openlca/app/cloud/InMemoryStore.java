package org.openlca.app.cloud;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openlca.core.model.ModelType;
import org.openlca.jsonld.EntityStore;
import org.openlca.jsonld.ModelPath;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class InMemoryStore implements EntityStore {

	private Map<ModelType, Map<String, JsonObject>> store = new HashMap<>();
	private Map<String, byte[]> resources = new HashMap<>();

	@Override
	public void close() throws IOException {
		store.clear();
	}

	@Override
	public void put(ModelType type, JsonObject object) {
		Map<String, JsonObject> substore = store.get(type);
		if (substore == null)
			store.put(type, substore = new HashMap<>());
		String refId = getRefId(object);
		substore.put(refId, object);
	}

	@Override
	public List<String> getRefIds(ModelType type) {
		Map<String, JsonObject> substore = store.get(type);
		if (substore == null)
			return Collections.emptyList();
		return new ArrayList<>(substore.keySet());
	}

	@Override
	public JsonObject get(ModelType type, String refId) {
		Map<String, JsonObject> substore = store.get(type);
		if (substore == null)
			return null;
		return substore.get(refId);
	}

	@Override
	public boolean contains(ModelType type, String refId) {
		Map<String, JsonObject> substore = store.get(type);
		if (substore == null)
			return false;
		return substore.containsKey(type.name() + "_" + refId);
	}

	private String getRefId(JsonObject object) {
		if (object == null)
			return null;
		JsonElement elem = object.get("@id");
		if (elem == null || !elem.isJsonPrimitive())
			return null;
		else
			return elem.getAsString();
	}

	@Override
	public void putBin(ModelType type, String refId, String filename,
			byte[] data) {
		String path = ModelPath.getBin(type, refId) + "/" + filename;
		put(path, data);
	}

	@Override
	public void put(String path, byte[] data) {
		resources.put(path, data);
	}

	@Override
	public byte[] get(String path) {
		return resources.get(path);
	}

	@Override
	public List<String> getBinFiles(ModelType type, String refId) {
		return null;
	}

}