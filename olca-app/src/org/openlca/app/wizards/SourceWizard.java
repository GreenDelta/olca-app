package org.openlca.app.wizards;

import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.core.database.BaseDao;
import org.openlca.core.model.Source;

public class SourceWizard extends AbstractWizard<Source> {

	@Override
	protected String getTitle() {
		return Messages.NewSource;
	}

	@Override
	protected BaseDao<Source> createDao() {
		return Database.createDao(Source.class);
	}

	@Override
	protected AbstractWizardPage<Source> createPage() {
		return new SourceWizardPage();
	}

}
