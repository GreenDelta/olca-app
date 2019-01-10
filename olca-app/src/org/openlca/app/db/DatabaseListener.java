package org.openlca.app.db;

import org.openlca.cloud.model.data.Dataset;
import org.openlca.cloud.util.Datasets;
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
		if (indexUpdater.disabled)
			return;
		Dataset dataset = toDataset(descriptor);
		if (dataset == null)
			return;
		indexUpdater.insert(dataset, descriptor.id);
	}

	@Override
	public void modelUpdated(BaseDescriptor descriptor) {
		if (indexUpdater.disabled)
			return;
		Dataset dataset = toDataset(descriptor);
		if (dataset == null)
			return;
		indexUpdater.update(dataset, descriptor.id);
	}

	@Override
	public void modelDeleted(BaseDescriptor descriptor) {
		if (indexUpdater.disabled)
			return;
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
		if (element.category != null) {
			category = categoryDao.getForId(element.category);
		}
		return Datasets.toDataset(element, category);
	}

}
