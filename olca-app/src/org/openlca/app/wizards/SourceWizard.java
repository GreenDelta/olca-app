package org.openlca.app.wizards;

import org.openlca.core.application.Messages;
import org.openlca.core.application.db.Database;
import org.openlca.core.database.BaseDao;
import org.openlca.core.model.Source;

public class SourceWizard extends AbstractWizard<Source> {

	@Override
	protected String getTitle() {
		return Messages.Sources_WizardTitle;
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
