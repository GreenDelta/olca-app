package org.openlca.app.db;

import org.openlca.app.cloud.index.Diff;
import org.openlca.app.cloud.index.DiffIndex;
import org.openlca.app.cloud.index.DiffType;
import org.openlca.cloud.model.data.Dataset;
import org.openlca.core.model.ModelType;

public class IndexUpdater {

	boolean disabled;
	private boolean inTransaction;

	public void beginTransaction() {
		// no multitransaction support implemented
		if (inTransaction)
			throw new IllegalStateException("A transaction is already running");
		inTransaction = true;
	}

	public void endTransaction() {
		if (!inTransaction)
			throw new IllegalStateException("No transaction running");
		DiffIndex index = getIndex();
		if (index != null)
			index.commit();
		inTransaction = false;
	}

	public void disable() {
		disabled = true;
	}

	public void enable() {
		disabled = false;
	}

	public void insert(Dataset dataset, long localId) {
		DiffIndex index = getIndex();
		if (index == null)
			return;
		insert(dataset, localId, index);
		if (inTransaction)
			return;
		index.commit();
	}

	private void insert(Dataset dataset, long localId, DiffIndex index) {
		index.add(dataset, localId);
		index.update(dataset, DiffType.NEW);
	}

	public void update(Dataset dataset, long localId) {
		DiffIndex index = getIndex();
		if (index == null)
			return;
		update(dataset, localId, index);
		if (inTransaction)
			return;
		index.commit();
	}

	private void update(Dataset dataset, long localId, DiffIndex index) {
		Diff existing = index.get(dataset);
		if (existing == null) {
			insert(dataset, localId);
			return;
		}
		// Parent categories are updated when child categories are added or
		// removed, this must not trigger a change
		if (dataset.type == ModelType.CATEGORY && dataset.equals(existing.getDataset()))
			return;
		DiffType previousType = existing.type;
		if (previousType == DiffType.NEW) {
			index.update(dataset, DiffType.NEW);
		} else {
			index.update(dataset, DiffType.CHANGED);
		}
	}
	
	public void delete(Dataset dataset) {
		DiffIndex index = getIndex();
		if (index == null)
			return;
		delete(dataset, index);
		if (inTransaction)
			return;
		index.commit();
	}

	private void delete(Dataset dataset, DiffIndex index) {
		index.update(dataset, DiffType.DELETED);
	}

	private DiffIndex getIndex() {
		if (disabled)
			return null;
		DiffIndex index = Database.getDiffIndex();
		return index;
	}

}
