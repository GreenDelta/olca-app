package org.openlca.app.wizards;

import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.core.database.BaseDao;
import org.openlca.core.model.ImpactMethod;

public class ImpactMethodWizard extends AbstractWizard<ImpactMethod> {

	@Override
	protected String getTitle() {
		return Messages.NewImpactMethod;
	}

	@Override
	protected BaseDao<ImpactMethod> createDao() {
		return Database.createDao(ImpactMethod.class);
	}

	@Override
	protected AbstractWizardPage<ImpactMethod> createPage() {
		return new ImpactMethodWizardPage();
	}

}
