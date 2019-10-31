package org.openlca.app.navigation.actions.cloud;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.openlca.app.cloud.index.DiffIndex;
import org.openlca.app.cloud.index.DiffType;
import org.openlca.app.cloud.ui.diff.DiffResult;
import org.openlca.app.db.Database;
import org.openlca.cloud.model.data.Dataset;
import org.openlca.core.database.CategorizedEntityDao;
import org.openlca.core.database.Daos;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.util.KeyGen;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Util {

	private final static Logger log = LoggerFactory.getLogger(Util.class);
	private DiffIndex index;
	private Map<String, Long> localIds = new HashMap<>();

	private Util(DiffIndex index) {
		this.index = index;
	}

	static void index(List<DiffResult> changed, DiffIndex index, Consumer<DiffResult> callback) {
		Util util = new Util(index);
		util.localIds = util.getLocalIds(changed);
		for (DiffResult diff : changed) {
			util.index(diff);
			if (callback == null)
				continue;
			callback.accept(diff);
		}
		index.commit();
	}

	private Map<String, Long> getLocalIds(List<DiffResult> changed) {
		Map<ModelType, Set<String>> refIds = groupRefIdsByModelType(changed);
		Map<String, Long> refIdToLocalId = new HashMap<>();
		IDatabase db = Database.get();
		for (ModelType type : refIds.keySet()) {
			CategorizedEntityDao<?, ?> dao = Daos.categorized(db, type);
			Set<String> ids = refIds.get(type);
			List<? extends CategorizedDescriptor> descriptors = dao.getDescriptorsForRefIds(ids);
			for (CategorizedDescriptor descriptor : descriptors) {
				ids.remove(descriptor.refId);
				refIdToLocalId.put(descriptor.refId, descriptor.id);
			}
			if (!ids.isEmpty() && type == ModelType.CATEGORY) {
				// some old category ids are in older repositories, to avoid an
				// index problem, the new ids are calculated
				Set<String> correctedIds = new HashSet<>();
				for (DiffResult r : changed) {
					if (r.local != null || r.remote.isDeleted() || !ids.contains(r.remote.refId))
						continue;
					String[] categories = r.remote.categories.toArray(new String[r.remote.categories.size()]);
					String[] path = Strings.prepend(categories, r.remote.categoryType.name());
					path = Strings.append(path, r.remote.name);
					String newId = KeyGen.get(path);
					r.remote.refId = newId;
					correctedIds.add(newId);
				}
				descriptors = dao.getDescriptorsForRefIds(correctedIds);
				for (CategorizedDescriptor descriptor : descriptors) {
					refIdToLocalId.put(descriptor.refId, descriptor.id);
				}
			}
		}
		return refIdToLocalId;
	}

	private Map<ModelType, Set<String>> groupRefIdsByModelType(List<DiffResult> changed) {
		Map<ModelType, Set<String>> refIds = new HashMap<>();
		for (DiffResult result : changed) {
			if (result.local != null || result.remote.isDeleted())
				continue;
			ModelType mType = result.getDataset().type;
			if (!mType.isCategorized())
				continue;
			Set<String> ids = refIds.get(mType);
			if (ids == null)
				refIds.put(mType, ids = new HashSet<>());
			ids.add(result.getDataset().refId);
		}
		return refIds;
	}

	private void index(DiffResult diff) {
		log.debug("Indexing: " + diff.toString());
		Dataset dataset = diff.getDataset();
		if (!dataset.type.isCategorized())
			return;
		if (diff.remote.isDeleted()) {
			if (diff.local != null) {
				if (diff.local.type == DiffType.NO_DIFF || diff.overwriteLocalChanges()) {
					index.remove(dataset.refId);
				} else if (diff.local.type.isOneOf(DiffType.NEW, DiffType.CHANGED)) {
					index.update(dataset, DiffType.NEW);
				}
			}
		} else if (diff.local == null) {
			index.add(dataset, localIds.get(dataset.refId));
		} else if (diff.overwriteLocalChanges()) {
			index.update(dataset, DiffType.NO_DIFF);
		}
	}

}
