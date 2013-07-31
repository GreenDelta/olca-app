package org.openlca.app.update;

public class IntermittentConnectionFailure extends RuntimeException {

	private static final long serialVersionUID = 1516567175354151077L;

	public IntermittentConnectionFailure() {
	}

	public IntermittentConnectionFailure(String message) {
		super(message);

	}

	public IntermittentConnectionFailure(Throwable cause) {
		super(cause);

	}

	public IntermittentConnectionFailure(String message, Throwable cause) {
		super(message, cause);

	}

}
