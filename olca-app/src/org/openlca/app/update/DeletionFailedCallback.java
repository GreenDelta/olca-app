package org.openlca.app.update;

public interface DeletionFailedCallback {

	public enum DeletionFailedResponse {
		ERROR, REPEAT, IGNORE;
	}

	DeletionFailedResponse deletionFailed(String path);

}
