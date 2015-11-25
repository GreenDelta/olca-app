package org.openlca.app.db;

import org.openlca.app.cloud.index.DiffIndex;
import org.openlca.app.cloud.index.DiffType;
import org.openlca.cloud.model.data.Dataset;

public class IndexUpdater {

	private boolean disabled;
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

	public void insert(Dataset dataset) {
		DiffIndex index = getIndex();
		if (index == null)
			return;
		insert(dataset, index);
		if (inTransaction)
			return;
		index.commit();
	}

	private void insert(Dataset dataset, DiffIndex index) {
		index.add(dataset);
		index.update(dataset, DiffType.NEW);
	}

	public void update(Dataset dataset) {
		DiffIndex index = getIndex();
		if (index == null)
			return;
		update(dataset, index);
		if (inTransaction)
			return;
		index.commit();
	}

	private void update(Dataset dataset, DiffIndex index) {
		DiffType previousType = index.get(dataset.getRefId()).type;
		if (previousType != DiffType.NEW)
			index.update(dataset, DiffType.CHANGED);
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
