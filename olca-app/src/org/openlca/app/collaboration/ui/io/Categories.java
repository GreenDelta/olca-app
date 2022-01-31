package org.openlca.app.collaboration.ui.io;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.openlca.core.model.ModelType;
import org.openlca.core.model.Version;
import org.openlca.git.find.Entries;
import org.openlca.git.model.Entry.EntryType;
import org.openlca.util.KeyGen;
import org.openlca.util.Strings;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

class Categories {

	private static final Gson gson = new Gson();
	private final Map<String, String> refIdToName = new HashMap<>();
	private final Map<String, ModelType> refIdToType = new HashMap<>();
	private final Map<String, String> refIdToParent = new HashMap<>();
	private final Map<String, String> pathToRefId = new HashMap<>();

	static Categories of(Entries entries, String commitId) {
		return new Categories(entries, commitId);
	}
	
	private Categories(Entries entries, String commitId) {
		init(entries, commitId, "");
	}

	private void init(Entries entries, String commitId, String path) {
		entries.find().commit(commitId).path(path).all().forEach(entry -> {
			if (entry.typeOfEntry == EntryType.DATASET)
				return;
			init(entries, commitId, entry.fullPath);
			if (entry.typeOfEntry != EntryType.CATEGORY)
				return;
			var refId = getRefId(entry.fullPath);
			refIdToName.put(refId, entry.name);
			refIdToType.put(refId, entry.type);
			if (!Strings.nullOrEmpty(entry.category)) {
				refIdToParent.put(refId, getRefId(entry.category));
			}
			pathToRefId.put(entry.fullPath, refId);
		});
	}

	private String getRefId(String path) {
		return KeyGen.get(path.split("/"));
	}

	byte[] getForPath(String path) {
		var json = getForRefId(pathToRefId.get(path));
		if (json == null)
			return null;
		try {
			return gson.toJson(json).getBytes("utf-8");
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	JsonObject getForRefId(String refId) {
		if (refId == null)
			return null;
		JsonObject category = new JsonObject();
		category.addProperty("@type", ModelType.CATEGORY.name());
		category.addProperty("@id", refId);
		category.addProperty("name", refIdToName.get(refId));
		category.addProperty("modelType", refIdToType.get(refId).name());
		category.addProperty("version", new Version(0).toString());
		category.addProperty("category", refIdToParent.get(refId));
		return category;
	}

}
