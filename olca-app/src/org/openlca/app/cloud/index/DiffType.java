package org.openlca.app.cloud.index;


public enum DiffType {

	NO_DIFF, NEW, CHANGED, DELETED;

	public boolean isOneOf(DiffType... types) {
		if (types == null)
			return false;
		for (DiffType type : types)
			if (type == this)
				return true;
		return false;
	}
	
}