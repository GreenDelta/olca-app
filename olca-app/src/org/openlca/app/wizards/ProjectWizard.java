package org.openlca.app.wizards;

import org.openlca.core.application.Messages;
import org.openlca.core.application.db.Database;
import org.openlca.core.database.BaseDao;
import org.openlca.core.model.Project;

public class ProjectWizard extends AbstractWizard<Project> {

	@Override
	protected String getTitle() {
		return Messages.Projects_WizardTitle;
	}

	@Override
	protected BaseDao<Project> createDao() {
		return Database.createDao(Project.class);
	}

	@Override
	protected AbstractWizardPage<Project> createPage() {
		return new ProjectWizardPage();
	}

}
