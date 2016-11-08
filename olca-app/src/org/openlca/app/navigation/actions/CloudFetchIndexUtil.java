package org.openlca.app.navigation.actions;

import static org.openlca.app.cloud.index.DiffType.CHANGED;
import static org.openlca.app.cloud.index.DiffType.DELETED;
import static org.openlca.app.cloud.index.DiffType.NEW;
import static org.openlca.app.cloud.index.DiffType.NO_DIFF;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openlca.app.cloud.index.DiffIndex;
import org.openlca.app.cloud.index.DiffType;
import org.openlca.app.cloud.ui.diff.DiffResult;
import org.openlca.app.cloud.ui.diff.DiffResult.DiffResponse;
import org.openlca.app.db.Database;
import org.openlca.cloud.model.data.Dataset;
import org.openlca.core.database.CategorizedEntityDao;
import org.openlca.core.database.Daos;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.CategorizedDescriptor;

class FetchIndexHelper {

	private DiffIndex index;
	private Map<String, Long> localIds = new HashMap<>();

	private FetchIndexHelper(DiffIndex index) {
		this.index = index;
	}

	static void index(List<DiffResult> changed, DiffIndex index) {
		FetchIndexHelper helper = new FetchIndexHelper(index);
		helper.localIds = getLocalIds(changed);
		for (DiffResult diff : changed)
			helper.index(diff);
		index.commit();
	}

	private static Map<String, Long> getLocalIds(List<DiffResult> changed) {
		Map<ModelType, Set<String>> refIds = groupRefIdsByModelType(changed);
		Map<String, Long> refIdToLocalId = new HashMap<>();
		IDatabase db = Database.get();
		for (ModelType type : refIds.keySet()) {
			CategorizedEntityDao<?, ? extends CategorizedDescriptor> dao = Daos
					.createCategorizedDao(db, type);
			List<? extends CategorizedDescriptor> descriptors = dao
					.getDescriptorsForRefIds(refIds.get(type));
			for (CategorizedDescriptor descriptor : descriptors)
				refIdToLocalId.put(descriptor.getRefId(), descriptor.getId());
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
			index.add(dataset, localIds.get(dataset.refId));
			index.update(dataset, NEW);
		} else {
			DiffType previousType = index.get(dataset.refId).type;
			if (previousType != NEW)
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
