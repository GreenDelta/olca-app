package org.openlca.app.wizards;

import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.core.database.BaseDao;
import org.openlca.core.model.UnitGroup;

public class UnitGroupWizard extends AbstractWizard<UnitGroup> {

	@Override
	protected String getTitle() {
		return Messages.NewUnitGroup;
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
