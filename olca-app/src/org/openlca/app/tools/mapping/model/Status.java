package org.openlca.app.tools.mapping.model;

/**
 * Status describes the state a mapping entry or flow reference can have when
 * when synchronized or applied on a database.
 */
public class Status {

	public static final int OK = 0;
	public static final int WARNING = 1;
	public static final int ERROR = 2;

	public final int type;
	public final String message;

	private Status(int type, String message) {
		this.type = type;
		this.message = message;
	}

	public static Status ok() {
		return ok("ok");
	}

	public static Status ok(String message) {
		return new Status(OK, message);
	}

	public static Status warn(String message) {
		return new Status(WARNING, message);
	}

	public static Status error(String message) {
		return new Status(ERROR, message);
	}

	public boolean isOk() {
		return type == OK;
	}

	public boolean isWarning() {
		return type == WARNING;
	}

	public boolean isError() {
		return type == ERROR;
	}
}
