package org.openlca.app.wizards;

import java.util.UUID;

import org.eclipse.swt.widgets.Composite;
import org.openlca.app.db.Database;
import org.openlca.core.database.BaseDao;
import org.openlca.core.model.CostCategory;

public class CostCategoryWizard extends AbstractWizard<CostCategory> {

	@Override
	protected BaseDao<CostCategory> createDao() {
		return Database.createDao(CostCategory.class);
	}

	@Override
	protected String getTitle() {
		return "#New cost category";
	}

	@Override
	protected AbstractWizardPage<CostCategory> createPage() {
		return new Page();
	}

	private class Page extends AbstractWizardPage<CostCategory> {

		Page() {
			super("CostCategoryWizardPage");
			setTitle("#New cost category");
			setPageComplete(false);
		}

		@Override
		protected void createContents(Composite container) {
		}

		@Override
		public CostCategory createModel() {
			CostCategory cc = new CostCategory();
			cc.setRefId(UUID.randomUUID().toString());
			cc.setName(getModelName());
			cc.setDescription(getModelDescription());
			return cc;
		}
	}
}
