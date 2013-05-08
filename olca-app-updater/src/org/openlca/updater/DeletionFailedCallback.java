package org.openlca.updater;

public interface DeletionFailedCallback {

	public enum DeletionFailedResponse {
		ERROR, REPEAT, IGNORE;
	}

	DeletionFailedResponse deletionFailed(String path);

}
