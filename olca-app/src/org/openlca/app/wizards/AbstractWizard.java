package org.openlca.app.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.App;
import org.openlca.app.db.Cache;
import org.openlca.app.rcp.ImageType;
import org.openlca.core.database.BaseDao;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.Category;
import org.openlca.core.model.descriptors.Descriptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractWizard<T extends CategorizedEntity> extends
		Wizard implements INewModelWizard {

	private Logger log = LoggerFactory.getLogger(getClass());
	private Category category;
	private AbstractWizardPage<T> page;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(getTitle());
		setDefaultPageImageDescriptor(ImageType.NEW_WIZARD.getDescriptor());
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
			model.setCategory(category);
			model.setLastChange(System.currentTimeMillis());
			createDao().insert(model);
			Cache.registerNew(Descriptors.toDescriptor(model));
			App.openEditor(model);
			return true;
		} catch (Exception e) {
			log.error("failed to create actor", e);
			return false;
		}
	}

	protected abstract String getTitle();

	protected abstract BaseDao<T> createDao();

	protected abstract AbstractWizardPage<T> createPage();

}
