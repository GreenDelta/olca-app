package org.openlca.app.navigation.actions.cloud;

import static org.openlca.app.cloud.index.DiffType.CHANGED;
import static org.openlca.app.cloud.index.DiffType.DELETED;
import static org.openlca.app.cloud.index.DiffType.NEW;
import static org.openlca.app.cloud.index.DiffType.NO_DIFF;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.openlca.app.cloud.index.DiffIndex;
import org.openlca.app.cloud.ui.diff.DiffResult;
import org.openlca.app.cloud.ui.diff.DiffResult.DiffResponse;
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

class FetchIndexHelper {

	private final static Logger log = LoggerFactory.getLogger(FetchIndexHelper.class);
	private DiffIndex index;
	private Map<String, Long> localIds = new HashMap<>();

	private FetchIndexHelper(DiffIndex index) {
		this.index = index;
	}

	static void index(List<DiffResult> changed, DiffIndex index, Consumer<DiffResult> callback) {
		FetchIndexHelper helper = new FetchIndexHelper(index);
		helper.localIds = getLocalIds(changed);
		for (DiffResult diff : changed) {
			helper.index(diff);
			if (callback == null)
				continue;
			callback.accept(diff);
		}
		index.commit();
	}

	private static Map<String, Long> getLocalIds(List<DiffResult> changed) {
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
					if (r.getType() != DiffResponse.ADD_TO_LOCAL || !ids.contains(r.remote.refId))
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

	private static Map<ModelType, Set<String>> groupRefIdsByModelType(
			List<DiffResult> changed) {
		Map<ModelType, Set<String>> refIds = new HashMap<>();
		for (DiffResult result : changed) {
			if (result.getType() != DiffResponse.ADD_TO_LOCAL)
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
		DiffResponse responseType = diff.getType();
		switch (responseType) {
		case NONE:
			if (bothDeleted(diff))
				index.remove(dataset.refId);
			else
				index.update(dataset, NO_DIFF);
			break;
		case MODIFY_IN_LOCAL:
			index.update(dataset, NO_DIFF);
			break;
		case ADD_TO_LOCAL:
			index.add(dataset, localIds.get(dataset.refId));
			break;
		case DELETE_FROM_LOCAL:
			index.remove(dataset.refId);
			break;
		case CONFLICT:
			indexConflict(diff);
			break;
		default:
			break;
		}
	}

	private void indexConflict(DiffResult diff) {
		if (diff.local.type == CHANGED)
			indexChanged(diff);
		else if (diff.local.type == NEW)
			indexChanged(diff);
		else if (diff.local.type == DELETED)
			indexDeleted(diff);
	}

	private void indexDeleted(DiffResult diff) {
		Dataset dataset = diff.getDataset();
		if (diff.overwriteRemoteChanges())
			index.update(dataset, DELETED);
		else if (diff.overwriteLocalChanges())
			index.update(dataset, NO_DIFF);
	}

	private void indexChanged(DiffResult diff) {
		if (diff.overwriteRemoteChanges())
			indexOverwritten(diff);
		else
			indexMerged(diff);
	}

	private void indexOverwritten(DiffResult diff) {
		Dataset dataset = diff.getDataset();
		if (diff.remote.isDeleted()) {
			index.update(dataset, NEW);
		} else {
			index.update(dataset, CHANGED);
		}
	}

	private void indexMerged(DiffResult diff) {
		Dataset dataset = diff.getDataset();
		if (diff.remote.isDeleted())
			index.remove(dataset.refId);
		else
			index.update(dataset, NO_DIFF);
	}

	private boolean bothDeleted(DiffResult diff) {
		if (diff.remote == null)
			return false;
		if (!diff.remote.isDeleted())
			return false;
		return diff.local == null;
	}

}
