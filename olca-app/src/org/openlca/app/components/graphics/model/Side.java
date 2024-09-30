package org.openlca.app.components.graphics.model;

public enum Side {
	INPUT,
	OUTPUT,
	BOTH;

	public Side opposite() {
		return this == INPUT ? OUTPUT : INPUT;
	}

}
