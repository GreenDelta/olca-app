package org.openlca.app.db;

import java.io.IOException;
import java.util.Collections;

import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.git.util.DiffEntries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkspaceIdUpdater {

	private final static Logger log = LoggerFactory.getLogger(WorkspaceIdUpdater.class);
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

	public void remove(RootDescriptor descriptor) {
		if (disabled || !Repository.isConnected())
			return;
		var workspaceIds = Repository.get().workspaceIds;
		var path = workspaceIds.getPath(Cache.getPathCache(), descriptor);
		workspaceIds.remove(path);
		if (inTransaction)
			return;
		flushObjectIdStore();
	}

	/**
	 * if a new model is created, the parent categories will be invalidated, if
	 * the new model is deleted the parent categories will still be invalidated,
	 * so we need to restore the previous object id
	 */
	public void restoreParents(ModelType type, Long id) {
		if (disabled || !Repository.isConnected())
			return;
		var repo = Repository.get();
		var workspaceIds = repo.workspaceIds;
		var path = type.name();
		if (id != null) {
			path += "/" + Cache.getPathCache().pathOf(id);
		}
		while (path != null) {
			try {
				var diffs = DiffEntries.workspace(repo.toConfig(), null, Collections.singletonList(path));
				if (!diffs.isEmpty()) {
					path = null;
				} else {
					workspaceIds.put(path, repo.ids.get(path));
					path = path.contains("/") ? path.substring(0, path.lastIndexOf("/")) : null;
				}
			} catch (IOException e) {
				log.error("Error restoring category id", e);
			}
		}
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
			var log = LoggerFactory.getLogger(getClass());
			log.error("Error flushing object ids", e);
		}
	}

}
