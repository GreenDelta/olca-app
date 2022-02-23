package org.openlca.app.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.App;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.core.database.BaseDao;
import org.openlca.core.database.Daos;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.descriptors.Descriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractWizard<T extends RootEntity> extends
		Wizard implements INewModelWizard {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private Category category;
	private AbstractWizardPage<T> page;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(getTitle());
	}

	public AbstractWizardPage<T> getPage() {
		return page;
	}

	@Override
	public void setCategory(Category category) {
		this.category = category;
	}

	protected Category getCategory() {
		return category;
	}

	@Override
	public void addPages() {
		page = createPage();
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		T model = page.createModel();
		log.trace("create {}", model);
		try {
			model.category = category;
			model.lastChange = System.currentTimeMillis();
			createDao().insert(model);
			var descriptor = Descriptor.of(model);
			Cache.registerNew(descriptor);
			App.open(model);
			return true;
		} catch (Exception e) {
			log.error("failed to create save " + model, e);
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	protected BaseDao<T> createDao() {
		return (BaseDao<T>) Daos.root(Database.get(), getModelType());
	}

	protected abstract String getTitle();

	protected abstract ModelType getModelType();

	protected abstract AbstractWizardPage<T> createPage();

}
