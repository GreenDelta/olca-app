package org.openlca.app.cloud;

import java.util.ArrayList;
import java.util.List;

import org.openlca.app.cloud.ui.DiffResult;
import org.openlca.app.db.Database;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Version;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.CategoryDescriptor;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.jsonld.EntityStore;
import org.openlca.jsonld.output.JsonExport;

import com.google.gson.JsonObject;
import com.greendelta.cloud.api.InMemoryStore;
import com.greendelta.cloud.api.RepositoryClient;
import com.greendelta.cloud.model.data.DatasetDescriptor;
import com.greendelta.cloud.util.WebRequests.WebRequestException;

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
				return new JsonObject();
			CategorizedEntity entity = load(result.local.getDescriptor());
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
				return new JsonObject();
			DatasetDescriptor descriptor = result.getDescriptor();
			try {
				return client.getDataset(descriptor.getType(),
						descriptor.getRefId());
			} catch (WebRequestException e) {
				return new JsonObject();
			}
		}

	}

}
