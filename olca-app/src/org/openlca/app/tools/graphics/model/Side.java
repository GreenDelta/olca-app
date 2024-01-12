package org.openlca.app.tools.graphics.model;

public enum Side {
	INPUT,
	OUTPUT,
	BOTH;

	public Side opposite() {
		return this == INPUT ? OUTPUT : INPUT;
	}

}
