package org.openlca.app.db;

import org.openlca.app.cloud.CloudUtil;
import org.openlca.cloud.model.data.Dataset;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.IDatabaseListener;
import org.openlca.core.model.Category;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.CategorizedDescriptor;

class DatabaseListener implements IDatabaseListener {

	private final IndexUpdater indexUpdater = new IndexUpdater();
	private final CategoryDao categoryDao;

	DatabaseListener(IDatabase database) {
		this.categoryDao = new CategoryDao(database);
	}

	IndexUpdater getIndexUpdater() {
		return indexUpdater;
	}

	@Override
	public void modelInserted(BaseDescriptor descriptor) {
		Dataset dataset = toDataset(descriptor);
		if (dataset == null)
			return;
		indexUpdater.insert(dataset, descriptor.getId());
	}

	@Override
	public void modelUpdated(BaseDescriptor descriptor) {
		Dataset dataset = toDataset(descriptor);
		if (dataset == null)
			return;
		indexUpdater.update(dataset, descriptor.getId());
	}

	@Override
	public void modelDeleted(BaseDescriptor descriptor) {
		Dataset dataset = toDataset(descriptor);
		if (dataset == null)
			return;
		indexUpdater.delete(dataset);
	}

	private Dataset toDataset(BaseDescriptor descriptor) {
		if (!(descriptor instanceof CategorizedDescriptor))
			return null;
		CategorizedDescriptor element = (CategorizedDescriptor) descriptor;
		Category category = null;
		if (element.getCategory() != null) {
			category = categoryDao.getForId(element.getCategory());
		}
		return CloudUtil.toDataset(element, category);
	}

}
