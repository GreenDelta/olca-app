package org.openlca.app.rcp.plugins;

public class MissingDependencyException extends Exception {

	private static final long serialVersionUID = 1270363433084619043L;

	public MissingDependencyException(String message) {
		super(message);
	}

	public MissingDependencyException(String message, Exception cause) {
		super(message, cause);
	}

	public MissingDependencyException(Exception cause) {
		super(cause);
	}

}
