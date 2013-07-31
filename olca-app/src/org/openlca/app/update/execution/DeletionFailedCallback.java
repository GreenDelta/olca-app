package org.openlca.app.update.execution;

public interface DeletionFailedCallback {

	public enum DeletionFailedResponse {
		ERROR, REPEAT, IGNORE;
	}

	DeletionFailedResponse deletionFailed(String path);

}
