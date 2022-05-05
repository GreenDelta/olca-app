package org.openlca.app.db;

import org.eclipse.jgit.lib.ObjectId;
import org.openlca.core.database.IDatabaseListener;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.RootDescriptor;

class DatabaseListener implements IDatabaseListener {

	private final WorkspaceIdUpdater workspaceIdsUpdater = new WorkspaceIdUpdater();

	WorkspaceIdUpdater getWorkspaceIdUpdater() {
		return workspaceIdsUpdater;
	}

	@Override
	public void modelInserted(Descriptor descriptor) {
		if (descriptor instanceof RootDescriptor d)
			workspaceIdsUpdater.remove(d);
	}

	@Override
	public void modelUpdated(Descriptor descriptor) {
		if (descriptor instanceof RootDescriptor d) {
			workspaceIdsUpdater.remove(d);
		}
	}

	@Override
	public void modelDeleted(Descriptor descriptor) {
		if (descriptor instanceof RootDescriptor d) {
			var workspaceIds = Repository.get().workspaceIds;
			ObjectId previousId = null;
			if (d.category != null) {
				var path = workspaceIds.getPath(Cache.getPathCache(), d);
				previousId = workspaceIds.get(path);
			} else {
				previousId = workspaceIds.get(descriptor.type);
			}
			workspaceIdsUpdater.remove(d);
			if (previousId != null && previousId.equals(ObjectId.zeroId())) {
				workspaceIdsUpdater.restoreParents(d.type, d.category);
			}
		}
	}

}
