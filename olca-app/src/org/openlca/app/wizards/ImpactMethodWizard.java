package org.openlca.app.wizards;

import java.util.UUID;

import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.core.database.BaseDao;
import org.openlca.core.model.ImpactMethod;

public class ImpactMethodWizard extends AbstractWizard<ImpactMethod> {

	@Override
	protected String getTitle() {
		return M.NewImpactMethod;
	}

	@Override
	protected BaseDao<ImpactMethod> createDao() {
		return Database.createDao(ImpactMethod.class);
	}

	@Override
	protected AbstractWizardPage<ImpactMethod> createPage() {
		return new Page();
	}

	private class Page extends AbstractWizardPage<ImpactMethod> {

		public Page() {
			super("LCIAMethodWizardPage");
			setTitle(M.NewImpactMethod);
			setMessage(M.CreatesANewImpactMethod);
			setPageComplete(false);
		}

		@Override
		protected void createContents(final Composite container) {
		}

		@Override
		public ImpactMethod createModel() {
			ImpactMethod method = new ImpactMethod();
			method.setRefId(UUID.randomUUID().toString());
			method.setName(getModelName());
			method.setDescription(getModelDescription());
			return method;
		}

	}
	
}
