package org.openlca.app.cloud.index;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.greendelta.cloud.model.data.DatasetIdentifier;

public class Diff implements Serializable {

	private static final long serialVersionUID = 252758475629265830L;
	public DatasetIdentifier identifier;
	public DatasetIdentifier changed;
	public DiffType type;
	Set<String> changedChildren = new HashSet<>();

	Diff(DatasetIdentifier identifier, DiffType type) {
		this.identifier = identifier;
		this.type = type;
	}

	public boolean hasChanged() {
		return type != DiffType.NO_DIFF;
	}

	public boolean childrenHaveChanged() {
		return !changedChildren.isEmpty();
	}

	public DatasetIdentifier getIdentifier() {
		if (changed != null)
			return changed;
		return identifier;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Diff))
			return false;
		Diff diff = (Diff) obj;
		return getIdentifier().getRefId().equals(
				diff.getIdentifier().getRefId());
	}

}