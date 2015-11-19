package org.openlca.app.db;

import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.cloud.index.DiffIndex;
import org.openlca.app.cloud.index.DiffType;
import org.openlca.cloud.model.data.DatasetDescriptor;
import org.openlca.core.database.IDatabaseListener;
import org.openlca.core.model.CategorizedEntity;

public class DatabaseListener implements IDatabaseListener {
	
	@Override
	public void modelInserted(Object object) {
		if (!(object instanceof CategorizedEntity))
			return;
		DiffIndex index = Database.getDiffIndex();
		if (index == null)
			return;
		CategorizedEntity entity = (CategorizedEntity) object;
		DatasetDescriptor descriptor = CloudUtil.toDescriptor(entity);
		index.add(descriptor);
		index.update(descriptor, DiffType.NEW);
	}

	@Override
	public void modelUpdated(Object object) {
		if (!(object instanceof CategorizedEntity))
			return;
		DiffIndex index = Database.getDiffIndex();
		if (index == null)
			return;
		CategorizedEntity entity = (CategorizedEntity) object;
		DatasetDescriptor descriptor = CloudUtil.toDescriptor(entity);
		DiffType previousType = index.get(descriptor.getRefId()).type;
		if (previousType != DiffType.NEW) {
			index.update(descriptor, DiffType.CHANGED);
			index.commit();
		}
	}

	@Override
	public void modelDeleted(Object object) {
		if (!(object instanceof CategorizedEntity))
			return;
		DiffIndex index = Database.getDiffIndex();
		if (index == null)
			return;
		CategorizedEntity entity = (CategorizedEntity) object;
		DatasetDescriptor descriptor = CloudUtil.toDescriptor(entity);
		index.update(descriptor, DiffType.DELETED);
		index.commit();
	}
}
