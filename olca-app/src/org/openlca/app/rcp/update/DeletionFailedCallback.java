package org.openlca.app.rcp.update;

public interface DeletionFailedCallback {

	public enum DeletionFailedResponse {
		ERROR, REPEAT, IGNORE;
	}

	DeletionFailedResponse deletionFailed(String path);

}
