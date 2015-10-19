package org.openlca.app.cloud.index;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.openlca.cloud.model.data.DatasetDescriptor;

public class Diff implements Serializable {

	private static final long serialVersionUID = 252758475629265830L;
	public DatasetDescriptor descriptor;
	public DatasetDescriptor changed;
	public DiffType type;
	Set<String> changedChildren = new HashSet<>();

	Diff(DatasetDescriptor descriptor, DiffType type) {
		this.descriptor = descriptor;
		this.type = type;
	}

	public boolean hasChanged() {
		return type != DiffType.NO_DIFF;
	}

	public boolean childrenHaveChanged() {
		return !changedChildren.isEmpty();
	}

	public DatasetDescriptor getDescriptor() {
		if (changed != null)
			return changed;
		return descriptor;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Diff))
			return false;
		Diff diff = (Diff) obj;
		return getDescriptor().getRefId().equals(
				diff.getDescriptor().getRefId());
	}

}