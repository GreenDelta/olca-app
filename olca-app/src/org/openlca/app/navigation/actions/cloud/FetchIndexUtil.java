package org.openlca.app.navigation.actions.cloud;

import static org.openlca.app.cloud.index.DiffType.CHANGED;
import static org.openlca.app.cloud.index.DiffType.DELETED;
import static org.openlca.app.cloud.index.DiffType.NEW;
import static org.openlca.app.cloud.index.DiffType.NO_DIFF;

import java.util.List;

import org.openlca.app.cloud.index.DiffIndex;
import org.openlca.app.cloud.index.DiffType;
import org.openlca.app.cloud.ui.DiffResult;
import org.openlca.app.cloud.ui.DiffResult.DiffResponse;
import org.openlca.cloud.model.data.Dataset;

class FetchIndexHelper {

	private DiffIndex index;

	private FetchIndexHelper(DiffIndex index) {
		this.index = index;
	}

	static void index(List<DiffResult> changed, DiffIndex index) {
		FetchIndexHelper helper = new FetchIndexHelper(index);
		for (DiffResult diff : changed)
			helper.index(diff);
		index.commit();
	}

	void index(DiffResult diff) {
		Dataset dataset = diff.getDataset();
		if (!dataset.getType().isCategorized())
			return;
		DiffResponse responseType = diff.getType();
		switch (responseType) {
		case NONE:
			if (bothDeleted(diff))
				index.remove(dataset.getRefId());
			else
				index.update(dataset, NO_DIFF);
			break;
		case MODIFY_IN_LOCAL:
			index.update(dataset, NO_DIFF);
			break;
		case ADD_TO_LOCAL:
			index.add(dataset);
			break;
		case DELETE_FROM_LOCAL:
			index.remove(dataset.getRefId());
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
			index.add(dataset);
			index.update(dataset, NEW);
		} else {
			DiffType previousType = index.get(dataset.getRefId()).type;
			if (previousType != NEW)
				index.update(dataset, CHANGED);
		}
	}

	private void indexMerged(DiffResult diff) {
		Dataset dataset = diff.getDataset();
		if (diff.remote.isDeleted())
			index.remove(dataset.getRefId());
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
