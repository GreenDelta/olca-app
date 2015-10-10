package org.openlca.app.cloud.index;

import java.util.List;

import com.greendelta.cloud.model.data.DatasetIdentifier;

public class DiffIndexer {

	private final DiffIndex index;

	public DiffIndexer(DiffIndex index) {
		this.index = index;
	}

	public void addToIndex(List<DatasetIdentifier> identifiers) {
		addToIndex(identifiers, null);
	}

	public void addToIndex(List<DatasetIdentifier> identifiers,
			DiffType diffType) {
		for (DatasetIdentifier identifier : identifiers)
			addToIndex(identifier, diffType);
		index.commit();
	}

	private void addToIndex(DatasetIdentifier identifier, DiffType diffType) {
		index.add(identifier);
		if (diffType != null && diffType != DiffType.NO_DIFF)
			index.update(identifier, diffType);
	}

	public void indexCreate(DatasetIdentifier identifier) {
		index.add(identifier);
		index.update(identifier, DiffType.NEW);
		index.commit();
	}

	public void indexModify(DatasetIdentifier identifier) {
		indexModify(identifier, false);
	}

	public void indexModify(DatasetIdentifier identifier, boolean forceOverwrite) {
		DiffType previousType = index.get(identifier.getRefId()).type;
		if (forceOverwrite || previousType != DiffType.NEW) {
			index.update(identifier, DiffType.CHANGED);
			index.commit();
		}
	}

	public void indexDelete(DatasetIdentifier identifier) {
		index.update(identifier, DiffType.DELETED);
		index.commit();
	}

	public void indexCommit(DatasetIdentifier identifier) {
		DiffType before = index.get(identifier.getRefId()).type;
		if (before == DiffType.DELETED) {
			index.remove(identifier.getRefId());
		} else
			index.update(identifier, DiffType.NO_DIFF);
		index.commit();
	}

	public Diff getDiff(DatasetIdentifier identifier) {
		if (identifier == null)
			return null;
		return index.get(identifier.getRefId());
	}

}
