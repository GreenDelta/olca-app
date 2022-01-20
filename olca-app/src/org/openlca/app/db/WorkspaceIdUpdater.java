package org.openlca.app.db;

import java.io.IOException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.openlca.core.model.descriptors.CategorizedDescriptor;

public class WorkspaceIdUpdater {

	private static final Logger log = LogManager.getLogger(WorkspaceIdUpdater.class);
	private boolean disabled;
	private boolean inTransaction;

	public void beginTransaction() {
		// no multitransaction support implemented
		if (inTransaction)
			throw new IllegalStateException("A transaction is already running");
		inTransaction = true;
	}

	public void endTransaction() {
		if (!inTransaction)
			throw new IllegalStateException("No transaction running");
		flushObjectIdStore();
		inTransaction = false;
	}

	public void disable() {
		disabled = true;
	}

	public void enable() {
		disabled = false;
	}

	public void invalidate(CategorizedDescriptor descriptor) {
		if (disabled || !Repository.isConnected())
			return;
		var workspaceIds = Repository.get().workspaceIds;
		var path = workspaceIds.getPath(Cache.getPathCache(), descriptor);
		workspaceIds.invalidate(path);
		if (inTransaction)
			return;
		flushObjectIdStore();
	}

	private void flushObjectIdStore() {
		if (!Repository.isConnected())
			return;
		try {
			Repository.get().workspaceIds.save();
		} catch (IOException e) {
			log.error("Error flushing object ids", e);
		}
	}

}
