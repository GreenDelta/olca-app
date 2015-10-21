package org.openlca.app.cloud;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openlca.app.cloud.ui.DiffResult;
import org.openlca.app.db.Database;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.model.data.DatasetDescriptor;
import org.openlca.cloud.util.WebRequests.WebRequestException;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Version;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.CategoryDescriptor;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.jsonld.EntityStore;
import org.openlca.jsonld.output.JsonExport;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class CloudUtil {

	public static List<DatasetDescriptor> toDescriptors(List<DiffResult> results) {
		List<DatasetDescriptor> descriptors = new ArrayList<>();
		for (DiffResult result : results)
			descriptors.add(result.getDescriptor());
		return descriptors;
	}
	
	public static DatasetDescriptor toDescriptor(CategorizedDescriptor entity,
			CategoryDescriptor category) {
		DatasetDescriptor descriptor = new DatasetDescriptor();
		descriptor.setRefId(entity.getRefId());
		descriptor.setType(entity.getModelType());
		descriptor.setVersion(Version.asString(entity.getVersion()));
		descriptor.setLastChange(entity.getLastChange());
		descriptor.setName(entity.getName());
		ModelType categoryType = null;
		if (category != null) {
			descriptor.setCategoryRefId(category.getRefId());
			categoryType = category.getCategoryType();
		} else {
			if (entity.getModelType() == ModelType.CATEGORY)
				categoryType = ((CategoryDescriptor) entity).getCategoryType();
			else
				categoryType = entity.getModelType();
		}
		descriptor.setCategoryType(categoryType);
		return descriptor;
	}

	public static DatasetDescriptor toDescriptor(CategorizedEntity entity) {
		return toDescriptor(Descriptors.toDescriptor(entity),
				Descriptors.toDescriptor(entity.getCategory()));
	}

	public static JsonLoader getJsonLoader(RepositoryClient client) {
		return new JsonLoader(client);
	}

	public static class JsonLoader {

		private final RepositoryClient client;

		private JsonLoader(RepositoryClient client) {
			this.client = client;
		}

		public JsonObject getLocalJson(DiffResult result) {
			if (result.local == null)
				return null;
			CategorizedEntity entity = load(result.local.getDescriptor());
			if (entity == null)
				return null;
			EntityStore store = new InMemoryStore();
			ModelType type = ModelType.forModelClass(entity.getClass());
			new JsonExport(null, store).write(entity, (message, data) -> {
			});
			return store.get(type, entity.getRefId());
		}

		private CategorizedEntity load(DatasetDescriptor descriptor) {
			return Database.createRootDao(descriptor.getType()).getForRefId(
					descriptor.getRefId());
		}

		public JsonObject getRemoteJson(DiffResult result) {
			if (result.remote != null && result.remote.isDeleted())
				return null;
			DatasetDescriptor descriptor = result.getDescriptor();
			try {
				return client.getDataset(descriptor.getType(),
						descriptor.getRefId());
			} catch (WebRequestException e) {
				return null;
			}
		}

	}

	private static class InMemoryStore implements EntityStore {

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
}
