package org.openlca.app.cloud;

import org.openlca.app.cloud.ui.DiffResult;
import org.openlca.app.cloud.ui.compare.JsonUtil;
import org.openlca.app.db.Database;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.model.data.DatasetDescriptor;
import org.openlca.cloud.util.WebRequests.WebRequestException;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ModelType;
import org.openlca.jsonld.EntityStore;
import org.openlca.jsonld.output.JsonExport;

import com.google.gson.JsonObject;

public class JsonLoader {

	private final RepositoryClient client;

	JsonLoader(RepositoryClient client) {
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
			JsonObject json = client.getDataset(descriptor.getType(),
					descriptor.getRefId());
			if (isImpactMethod(json))
				appendImpactMethodObjects(json);
			return json;
		} catch (WebRequestException e) {
			return null;
		}
	}

	private boolean isImpactMethod(JsonObject json) {
		String type = JsonUtil.getType(json);
		return ImpactMethod.class.getSimpleName().equals(type);
	}

	private void appendImpactMethodObjects(JsonObject json) {
		// TODO
	}

}