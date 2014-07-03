package org.openlca.app.wizards;

import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.core.database.BaseDao;
import org.openlca.core.model.Process;

public class ProcessWizard extends AbstractWizard<Process> {

	@Override
	protected String getTitle() {
		return Messages.NewProcess;
	}

	@Override
	protected BaseDao<Process> createDao() {
		return Database.createDao(Process.class);
	}

	@Override
	protected AbstractWizardPage<Process> createPage() {
		return new ProcessWizardPage();
	}

}