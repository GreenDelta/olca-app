package org.openlca.app.db;

import org.openlca.app.cloud.CloudUtil;
import org.openlca.core.database.IDatabaseListener;
import org.openlca.core.model.CategorizedEntity;

class DatabaseListener implements IDatabaseListener {

	private IndexUpdater indexUpdater = new IndexUpdater();

	IndexUpdater getIndexUpdater() {
		return indexUpdater;
	}
	
	@Override
	public void modelInserted(Object object) {
		if (!(object instanceof CategorizedEntity))
			return;
		CategorizedEntity entity = (CategorizedEntity) object;
		indexUpdater.insert(CloudUtil.toDescriptor(entity));
	}

	@Override
	public void modelUpdated(Object object) {
		if (!(object instanceof CategorizedEntity))
			return;
		CategorizedEntity entity = (CategorizedEntity) object;
		indexUpdater.update(CloudUtil.toDescriptor(entity));
	}

	@Override
	public void modelDeleted(Object object) {
		if (!(object instanceof CategorizedEntity))
			return;
		CategorizedEntity entity = (CategorizedEntity) object;
		indexUpdater.delete(CloudUtil.toDescriptor(entity));
	}
}
