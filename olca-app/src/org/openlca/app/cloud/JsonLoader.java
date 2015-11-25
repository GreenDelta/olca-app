package org.openlca.app.cloud;

import org.openlca.app.cloud.ui.DiffResult;
import org.openlca.app.cloud.ui.compare.JsonUtil;
import org.openlca.app.db.Database;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.model.data.Dataset;
import org.openlca.cloud.util.WebRequests.WebRequestException;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ModelType;
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
		CategorizedEntity entity = load(result.local.getDataset());
		if (entity == null)
			return null;
		return JsonExport.toJson(entity);
	}

	private CategorizedEntity load(Dataset dataset) {
		ModelType type = dataset.getType();
		String refId = dataset.getRefId();
		return Database.createRootDao(type).getForRefId(refId);
	}

	public JsonObject getRemoteJson(DiffResult result) {
		if (result.remote != null && result.remote.isDeleted())
			return null;
		Dataset dataset = result.getDataset();
		try {
			JsonObject json = client.getDataset(dataset.getType(),
					dataset.getRefId());
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