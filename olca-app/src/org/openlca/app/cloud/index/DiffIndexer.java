package org.openlca.app.cloud.index;

import java.util.List;

import com.greendelta.cloud.model.data.DatasetDescriptor;

public class DiffIndexer {

	private final DiffIndex index;

	public DiffIndexer(DiffIndex index) {
		this.index = index;
	}

	public void addToIndex(List<DatasetDescriptor> descriptors) {
		addToIndex(descriptors, null);
	}

	public void addToIndex(List<DatasetDescriptor> descriptors,
			DiffType diffType) {
		for (DatasetDescriptor descriptor : descriptors)
			addToIndex(descriptor, diffType);
		index.commit();
	}

	private void addToIndex(DatasetDescriptor descriptor, DiffType diffType) {
		index.add(descriptor);
		if (diffType != null && diffType != DiffType.NO_DIFF)
			index.update(descriptor, diffType);
	}

	public void indexCreate(DatasetDescriptor descriptor) {
		index.add(descriptor);
		index.update(descriptor, DiffType.NEW);
		index.commit();
	}

	public void indexModify(DatasetDescriptor descriptor) {
		indexModify(descriptor, false);
	}

	public void indexModify(DatasetDescriptor descriptor, boolean forceOverwrite) {
		DiffType previousType = index.get(descriptor.getRefId()).type;
		if (forceOverwrite || previousType != DiffType.NEW) {
			index.update(descriptor, DiffType.CHANGED);
			index.commit();
		}
	}

	public void indexDelete(DatasetDescriptor descriptor) {
		index.update(descriptor, DiffType.DELETED);
		index.commit();
	}

	public void indexCommit(DatasetDescriptor descriptor) {
		DiffType before = index.get(descriptor.getRefId()).type;
		if (before == DiffType.DELETED) {
			index.remove(descriptor.getRefId());
		} else
			index.update(descriptor, DiffType.NO_DIFF);
		index.commit();
	}

	public Diff getDiff(DatasetDescriptor descriptor) {
		if (descriptor == null)
			return null;
		return index.get(descriptor.getRefId());
	}

}
