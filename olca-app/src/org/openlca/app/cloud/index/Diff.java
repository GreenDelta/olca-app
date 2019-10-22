package org.openlca.app.cloud.index;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.openlca.cloud.model.data.Dataset;

public class Diff implements Serializable {

	private static final long serialVersionUID = 252758475629265830L;
	public long localId;
	public Dataset dataset;
	public Dataset changed;
	public DiffType type;
	Set<String> changedChildren = new HashSet<>();

	Diff(Dataset descriptor, DiffType type) {
		this.dataset = descriptor;
		this.type = type;
	}

	public boolean hasChanged() {
		return type != DiffType.UNTRACKED && type != DiffType.NO_DIFF;
	}

	public boolean childrenHaveChanged() {
		return !changedChildren.isEmpty();
	}

	public Dataset getDataset() {
		if (changed != null)
			return changed;
		return dataset;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Diff))
			return false;
		Diff diff = (Diff) obj;
		return getDataset().refId.equals(diff.getDataset().refId);
	}

	public Diff copy() {
		Diff diff = new Diff(dataset, type);
		diff.changed = changed;
		diff.localId = localId;
		return diff;
	}

}