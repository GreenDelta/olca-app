package org.openlca.app.wizards;

import org.openlca.core.application.Messages;
import org.openlca.core.application.db.Database;
import org.openlca.core.database.BaseDao;
import org.openlca.core.model.UnitGroup;

public class UnitGroupWizard extends AbstractWizard<UnitGroup> {

	@Override
	protected String getTitle() {
		return Messages.Units_WizardTitle;
	}

	@Override
	protected BaseDao<UnitGroup> createDao() {
		return Database.createDao(UnitGroup.class);
	}

	@Override
	protected AbstractWizardPage<UnitGroup> createPage() {
		return new UnitGroupWizardPage();
	}

}
