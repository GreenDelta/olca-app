package org.openlca.app.db;

import org.openlca.app.cloud.index.DiffIndex;
import org.openlca.app.cloud.index.DiffType;
import org.openlca.cloud.model.data.DatasetDescriptor;

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

	public void insert(DatasetDescriptor descriptor) {
		DiffIndex index = getIndex();
		if (index == null)
			return;
		insert(descriptor, index);
		if (inTransaction)
			return;
		index.commit();
	}

	private void insert(DatasetDescriptor descriptor, DiffIndex index) {
		index.add(descriptor);
		index.update(descriptor, DiffType.NEW);
	}

	public void update(DatasetDescriptor descriptor) {
		DiffIndex index = getIndex();
		if (index == null)
			return;
		update(descriptor, index);
		if (inTransaction)
			return;
		index.commit();
	}

	private void update(DatasetDescriptor descriptor, DiffIndex index) {
		DiffType previousType = index.get(descriptor.getRefId()).type;
		if (previousType != DiffType.NEW)
			index.update(descriptor, DiffType.CHANGED);
	}

	public void delete(DatasetDescriptor descriptor) {
		DiffIndex index = getIndex();
		if (index == null)
			return;
		delete(descriptor, index);
		if (inTransaction)
			return;
		index.commit();
	}

	private void delete(DatasetDescriptor descriptor, DiffIndex index) {
		index.update(descriptor, DiffType.DELETED);
	}

	private DiffIndex getIndex() {
		if (disabled)
			return null;
		DiffIndex index = Database.getDiffIndex();
		return index;
	}

}
