package org.openlca.core.database.util;

public class ScriptException extends Exception {

	private static final long serialVersionUID = 1L;

	public ScriptException() {
	}

	public ScriptException(String message) {
		super(message);
	}

	public ScriptException(Throwable cause) {
		super(cause);
	}

	public ScriptException(String message, Throwable cause) {
		super(message, cause);
	}

}
