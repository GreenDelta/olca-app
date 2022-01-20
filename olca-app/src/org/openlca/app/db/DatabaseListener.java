package org.openlca.app.db;

import org.openlca.core.database.IDatabaseListener;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.Descriptor;

class DatabaseListener implements IDatabaseListener {

	private final WorkspaceIdUpdater workspaceIdsUpdater = new WorkspaceIdUpdater();

	WorkspaceIdUpdater getWorkspaceIdUpdater() {
		return workspaceIdsUpdater;
	}

	@Override
	public void modelInserted(Descriptor descriptor) {
		invalidateId(descriptor);
	}

	@Override
	public void modelUpdated(Descriptor descriptor) {
		invalidateId(descriptor);
	}

	@Override
	public void modelDeleted(Descriptor descriptor) {
		invalidateId(descriptor);
	}

	private void invalidateId(Descriptor descriptor) {
		if (descriptor instanceof CategorizedDescriptor d) {
			workspaceIdsUpdater.invalidate(d);
		}
	}

}
