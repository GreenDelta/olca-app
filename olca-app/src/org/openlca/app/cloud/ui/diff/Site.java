package org.openlca.app.cloud.ui.diff;

public enum Site {

	LOCAL, REMOTE;

	public Site getOther() {
		if (this == LOCAL)
			return REMOTE;
		return LOCAL;
	}

}