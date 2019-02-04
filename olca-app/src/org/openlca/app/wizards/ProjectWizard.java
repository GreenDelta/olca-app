package org.openlca.app.wizards;

import java.util.Calendar;
import java.util.UUID;

import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Project;

public class ProjectWizard extends AbstractWizard<Project> {

	@Override
	protected String getTitle() {
		return M.NewProject;
	}

	@Override
	protected AbstractWizardPage<Project> createPage() {
		return new ProjectWizardPage();
	}

	@Override
	protected ModelType getModelType() {
		return ModelType.PROJECT;
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
			Project p = new Project();
			p.refId = UUID.randomUUID().toString();
			p.name = getModelName();
			p.description = getModelDescription();
			p.creationDate = Calendar.getInstance().getTime();
			p.lastModificationDate = Calendar.getInstance().getTime();
			return p;
		}
	}
}
