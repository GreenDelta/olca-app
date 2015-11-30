package org.openlca.app.cloud;

import java.util.List;

import org.openlca.app.cloud.ui.compare.json.JsonUtil;
import org.openlca.app.db.Database;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.model.data.Dataset;
import org.openlca.cloud.util.WebRequests.WebRequestException;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.RootEntity;
import org.openlca.jsonld.output.JsonExport;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonLoader {

	private final RepositoryClient client;

	JsonLoader(RepositoryClient client) {
		this.client = client;
	}

	public JsonObject getLocalJson(Dataset dataset) {
		if (dataset == null)
			return null;
		CategorizedEntity entity = load(dataset);
		if (entity == null)
			return null;
		JsonObject json = JsonExport.toJson(entity, Database.get());
		if (entity instanceof ImpactMethod) {
			ImpactMethod method = (ImpactMethod) entity;
			replaceReferences(json, "impactCategories",
					method.getImpactCategories());
			replaceReferences(json, "nwSets", method.getNwSets());
		}
		return json;
	}

	private CategorizedEntity load(Dataset dataset) {
		ModelType type = dataset.getType();
		String refId = dataset.getRefId();
		return Database.createRootDao(type).getForRefId(refId);
	}

	public JsonObject getRemoteJson(Dataset dataset) {
		if (dataset == null)
			return null;
		try {
			JsonObject json = client.getDataset(dataset.getType(),
					dataset.getRefId());
			String type = JsonUtil.getString(json, "@type");
			if (ImpactMethod.class.getSimpleName().equals(type)) {
				replaceReferences(json, "impactCategories",
						ModelType.IMPACT_CATEGORY);
				replaceReferences(json, "nwSets", ModelType.NW_SET);
			}
			return json;
		} catch (WebRequestException e) {
			return null;
		}
	}

	private void replaceReferences(JsonObject obj, String field,
			List<? extends RootEntity> entities) {
		if (!obj.has(field))
			return;
		JsonArray array = new JsonArray();
		for (RootEntity entity : entities)
			array.add(JsonExport.toJson(entity, Database.get()));
		obj.add(field, array);
	}

	private void replaceReferences(JsonObject obj, String field, ModelType type) {
		if (!obj.has(field))
			return;
		JsonArray array = obj.getAsJsonArray(field);
		JsonArray replaced = new JsonArray();
		for (JsonElement element : array)
			try {
				JsonObject o = element.getAsJsonObject();
				String refId = o.get("@id").getAsString();
				JsonObject json = client.getDataset(type, refId);
				replaced.add(json);
			} catch (WebRequestException e) {
				// ignore
			}
		obj.add(field, replaced);
	}

}