package org.openlca.app.cloud.index;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.db.Database;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.model.data.Dataset;
import org.openlca.cloud.model.data.FetchRequestData;
import org.openlca.cloud.util.WebRequests.WebRequestException;
import org.openlca.core.database.CategorizedEntityDao;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.Daos;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Version;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.util.Strings;

public class IndexSync {

	private IDatabase database;
	private DiffIndex index;
	private RepositoryClient client;
	private CategoryDao categoryDao;

	public void run() {
		Map<ModelType, Map<String, FetchRequestData>> datasets = init();
		if (datasets == null)
			return;
		for (ModelType type : ModelType.values()) {
			if (!type.isCategorized())
				continue;
			Map<String, CategorizedDescriptor> descriptorMap = getDescriptors(type);
			Map<String, FetchRequestData> dataMap = datasets.get(type);
			if (dataMap != null) {
				for (FetchRequestData data : dataMap.values()) {
					CategorizedDescriptor descriptor = descriptorMap.get(data.refId);
					if (descriptor == null && !data.isDeleted()) {
						put(data.asDataset());
					} else {
						put(data.asDataset(), descriptor);
					}
				}
			}
			if (type == ModelType.PARAMETER)
				continue; // handle global parameters seperately
			for (CategorizedDescriptor descriptor : descriptorMap.values()) {
				if (dataMap != null && dataMap.containsKey(descriptor.getRefId()))
					continue;
				put(descriptor);
			}
		}
		Map<String, FetchRequestData> dataMap = datasets.get(ModelType.PARAMETER);
		for (CategorizedDescriptor descriptor : new ParameterDao(database).getGlobalDescriptors()) {
			if (dataMap != null && dataMap.containsKey(descriptor.getRefId()))
				continue;
			put(descriptor);
		}
		index.commit();
	}

	private Map<ModelType, Map<String, FetchRequestData>> init() {
		database = Database.get();
		if (database == null)
			return null;
		client = Database.getRepositoryClient();
		if (client == null)
			return null;
		index = Database.getDiffIndex();
		if (index == null)
			return null;
		categoryDao = new CategoryDao(database);
		index.clear();
		if (client.getConfig().getLastCommitId() == null)
			return new HashMap<>();
		try {
			Map<ModelType, Map<String, FetchRequestData>> mapped = new HashMap<>();
			Set<FetchRequestData> data = client.sync(client.getConfig().getLastCommitId());
			for (FetchRequestData d : data) {
				Map<String, FetchRequestData> forType = mapped.get(d.type);
				if (forType == null) {
					mapped.put(d.type, forType = new HashMap<>());
				}
				forType.put(d.refId, d);
			}
			return mapped;
		} catch (WebRequestException e) {
			return null;
		}
	}

	private Map<String, CategorizedDescriptor> getDescriptors(ModelType type) {
		CategorizedEntityDao<?, ?> dao = Daos.categorized(database, type);
		Map<String, CategorizedDescriptor> descriptors = new HashMap<>();
		for (CategorizedDescriptor descriptor : dao.getDescriptors()) {
			descriptors.put(descriptor.getRefId(), descriptor);
		}
		return descriptors;
	}

	private void put(Dataset dataset) {
		index.add(dataset, 0);
		index.update(dataset, DiffType.DELETED);
	}

	private void put(Dataset dataset, CategorizedDescriptor descriptor) {
		index.add(dataset, descriptor.getId());
		if (areEqual(dataset, descriptor))
			return;
		index.update(dataset, DiffType.CHANGED);
	}

	private void put(CategorizedDescriptor descriptor) {
		Category category = null;
		if (descriptor.getCategory() != null) {
			category = categoryDao.getForId(descriptor.getCategory());
		}
		Dataset dataset = CloudUtil.toDataset(descriptor, category);
		index.add(dataset, descriptor.getId());
		index.update(dataset, DiffType.NEW);
	}

	private boolean areEqual(Dataset dataset, CategorizedDescriptor descriptor) {
		if (!dataset.refId.equals(descriptor.getRefId()))
			return false;
		if (dataset.type != descriptor.getModelType())
			return false;
		if (!Strings.nullOrEqual(dataset.version, Version.asString(descriptor.getVersion())))
			return false;
		if (dataset.lastChange != descriptor.getLastChange())
			return false;
		return true;
	}

}
