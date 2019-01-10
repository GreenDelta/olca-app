package org.openlca.app.cloud.index;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.openlca.app.db.Database;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.model.data.Dataset;
import org.openlca.cloud.model.data.FetchRequestData;
import org.openlca.cloud.util.Datasets;
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

public class Reindexing {

	private IDatabase database;
	private DiffIndex index;
	private RepositoryClient client;
	private CategoryDao categoryDao;

	public static void execute() {
		new Reindexing().run();
	}

	private void run() {
		Map<ModelType, Map<String, FetchRequestData>> datasets = init();
		if (datasets == null)
			return;
		for (ModelType type : ModelType.values()) {
			if (!type.isCategorized())
				continue;
			Map<String, CategorizedDescriptor> descriptorMap = getDescriptors(type);
			Map<String, FetchRequestData> dataMap = datasets.get(type);
			if (dataMap != null) {
				remoteSync(dataMap.values(), descriptorMap);
			}
			if (type == ModelType.PARAMETER) {
				localSync(dataMap, new ParameterDao(database).getGlobalDescriptors());
			} else {
				localSync(dataMap, descriptorMap.values());
			}
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
			return initDataMap();
		} catch (WebRequestException e) {
			return null;
		}
	}

	private Map<ModelType, Map<String, FetchRequestData>> initDataMap() throws WebRequestException {
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
	}

	private void remoteSync(Collection<FetchRequestData> datasets, Map<String, CategorizedDescriptor> descriptorMap) {
		for (FetchRequestData data : datasets) {
			CategorizedDescriptor descriptor = descriptorMap.get(data.refId);
			if (descriptor == null) {
				if (!data.isDeleted()) {
					put(data.asDataset());
				}
			} else {
				put(data.asDataset(), descriptor, data.isDeleted());
			}
		}
	}

	private void localSync(Map<String, FetchRequestData> dataMap,
			Collection<? extends CategorizedDescriptor> descriptors) {
		for (CategorizedDescriptor descriptor : descriptors) {
			if (dataMap != null && dataMap.containsKey(descriptor.refId))
				continue;
			put(descriptor);
		}
	}

	private Map<String, CategorizedDescriptor> getDescriptors(ModelType type) {
		CategorizedEntityDao<?, ?> dao = Daos.categorized(database, type);
		Map<String, CategorizedDescriptor> descriptors = new HashMap<>();
		for (CategorizedDescriptor descriptor : dao.getDescriptors()) {
			descriptors.put(descriptor.refId, descriptor);
		}
		return descriptors;
	}

	private void put(Dataset dataset) {
		index.add(dataset, 0);
		index.update(dataset, DiffType.DELETED);
	}

	private void put(Dataset dataset, CategorizedDescriptor descriptor, boolean deletedOnRemote) {
		index.add(dataset, descriptor.id);
		if (deletedOnRemote) {
			index.update(dataset, DiffType.NEW);
			return;
		}
		if (areEqual(dataset, descriptor))
			return;
		index.update(dataset, DiffType.CHANGED);
	}

	private void put(CategorizedDescriptor descriptor) {
		Category category = null;
		if (descriptor.category != null) {
			category = categoryDao.getForId(descriptor.category);
		}
		Dataset dataset = Datasets.toDataset(descriptor, category);
		index.add(dataset, descriptor.id);
		index.update(dataset, DiffType.NEW);
	}

	private boolean areEqual(Dataset dataset, CategorizedDescriptor descriptor) {
		if (!dataset.refId.equals(descriptor.refId))
			return false;
		if (dataset.type != descriptor.type)
			return false;
		if (!Strings.nullOrEqual(dataset.version, Version.asString(descriptor.version)))
			return false;
		if (dataset.lastChange != descriptor.lastChange)
			return false;
		return true;
	}

}
