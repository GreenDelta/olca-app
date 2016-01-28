package org.openlca.app.wizards;

import java.util.Calendar;
import java.util.UUID;

import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.core.database.BaseDao;
import org.openlca.core.model.Project;

public class ProjectWizard extends AbstractWizard<Project> {

	@Override
	protected String getTitle() {
		return M.NewProject;
	}

	@Override
	protected BaseDao<Project> createDao() {
		return Database.createDao(Project.class);
	}

	@Override
	protected AbstractWizardPage<Project> createPage() {
		return new ProjectWizardPage();
	}

	private class ProjectWizardPage extends AbstractWizardPage<Project> {

		public ProjectWizardPage() {
			super("ProjectWizardPage");
			setTitle(M.NewProject);
			setMessage(M.CreatesANewProject);
			setPageComplete(false);
		}

		@Override
		protected void createContents(final Composite container) {
		}

		@Override
		public Project createModel() {
			Project project = new Project();
			project.setRefId(UUID.randomUUID().toString());
			project.setName(getModelName());
			project.setDescription(getModelDescription());
			project.setCreationDate(Calendar.getInstance().getTime());
			project.setLastModificationDate(Calendar.getInstance().getTime());
			return project;
		}

	}

}
