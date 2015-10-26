package org.openlca.app.cloud.index;

import java.util.Collections;
import java.util.List;

import org.openlca.cloud.model.data.DatasetDescriptor;
import org.openlca.core.model.ModelType;

public class DiffIndexer {

	private final DiffIndex index;

	public DiffIndexer(DiffIndex index) {
		this.index = index;
	}

	public void addToIndex(DatasetDescriptor descriptor) {
		addToIndex(descriptor, null);
	}

	public void addToIndex(DatasetDescriptor descriptor, DiffType diffType) {
		addToIndex(Collections.singletonList(descriptor), diffType);
	}

	public void addToIndex(List<DatasetDescriptor> descriptors) {
		addToIndex(descriptors, null);
	}

	public void addToIndex(List<DatasetDescriptor> descriptors,
			DiffType diffType) {
		if (descriptors.isEmpty())
			return;
		for (DatasetDescriptor descriptor : descriptors)
			index.add(descriptor);
		if (diffType != null && diffType != DiffType.NO_DIFF)
			for (DatasetDescriptor descriptor : descriptors)
				index.update(descriptor, diffType);
		index.commit();
	}

	public void indexCreate(DatasetDescriptor descriptor) {
		indexCreate(Collections.singletonList(descriptor));
	}

	public void indexCreate(List<DatasetDescriptor> descriptors) {
		if (descriptors.isEmpty())
			return;
		for (DatasetDescriptor descriptor : descriptors)
			index.add(descriptor);
		for (DatasetDescriptor descriptor : descriptors)
			index.update(descriptor, DiffType.NEW);
		index.commit();
	}

	public void indexModify(DatasetDescriptor descriptor) {
		indexModify(descriptor, false);
	}

	public void indexModify(DatasetDescriptor descriptor, boolean forceOverwrite) {
		indexModify(Collections.singletonList(descriptor), forceOverwrite);
	}

	public void indexModify(List<DatasetDescriptor> descriptors) {
		indexModify(descriptors, false);
	}

	public void indexModify(List<DatasetDescriptor> descriptors,
			boolean forceOverwrite) {
		if (descriptors.isEmpty())
			return;
		boolean updated = false;
		for (DatasetDescriptor descriptor : descriptors) {
			DiffType previousType = index.get(descriptor.getRefId()).type;
			if (forceOverwrite || previousType != DiffType.NEW) {
				index.update(descriptor, DiffType.CHANGED);
				updated = true;
			}
		}
		if (updated)
			index.commit();
	}

	public void indexDelete(DatasetDescriptor descriptor) {
		indexDelete(Collections.singletonList(descriptor));
	}

	public void indexDelete(List<DatasetDescriptor> descriptors) {
		if (descriptors.isEmpty())
			return;
		for (DatasetDescriptor descriptor : descriptors)
			index.update(descriptor, DiffType.DELETED);
		index.commit();
	}

	public void indexCommit(DatasetDescriptor descriptor) {
		indexCommit(Collections.singletonList(descriptor));
	}

	public void indexCommit(List<DatasetDescriptor> descriptors) {
		if (descriptors.isEmpty())
			return;
		for (DatasetDescriptor descriptor : descriptors) {
			DiffType before = index.get(descriptor.getRefId()).type;
			if (before == DiffType.DELETED)
				index.remove(descriptor.getRefId());
			else
				index.update(descriptor, DiffType.NO_DIFF);
		}
		index.commit();
	}

	public void indexFetch(DatasetDescriptor descriptor) {
		indexFetch(Collections.singletonList(descriptor));
	}

	public void indexFetch(List<DatasetDescriptor> descriptors) {
		if (descriptors.isEmpty())
			return;
		for (DatasetDescriptor descriptor : descriptors)
			index.update(descriptor, DiffType.NO_DIFF);
		index.commit();
	}

	public void removeFromIndex(DatasetDescriptor descriptor) {
		removeFromIndex(Collections.singletonList(descriptor));
	}

	public void removeFromIndex(List<DatasetDescriptor> descriptors) {
		if (descriptors.isEmpty())
			return;
		for (DatasetDescriptor descriptor : descriptors)
			index.remove(descriptor.getRefId());
		index.commit();
	}

	public boolean hasDeletedChildren(ModelType type) {
		for (Diff diff : index.getChanged())
			if (diff.type == DiffType.DELETED)
				if (diff.getDescriptor().getCategoryRefId() == null
						&& diff.getDescriptor().getType() == type)
					return true;
		return false;
	}

	public boolean hasChanges() {
		return index.getChanged().size() != 0;
	}

	public Diff getDiff(DatasetDescriptor descriptor) {
		if (descriptor == null)
			return null;
		return index.get(descriptor.getRefId());
	}

}
