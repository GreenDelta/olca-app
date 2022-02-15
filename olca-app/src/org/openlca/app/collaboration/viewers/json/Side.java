package org.openlca.app.collaboration.viewers.json;

public enum Side {

	LOCAL, REMOTE;

	public Side getOther() {
		if (this == LOCAL)
			return REMOTE;
		return LOCAL;
	}

}