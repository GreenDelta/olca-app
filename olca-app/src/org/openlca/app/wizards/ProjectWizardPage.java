package org.openlca.app.wizards;

import java.util.Calendar;
import java.util.UUID;

import org.eclipse.swt.widgets.Composite;
import org.openlca.app.Messages;
import org.openlca.app.rcp.ImageType;
import org.openlca.core.model.Project;

class ProjectWizardPage extends AbstractWizardPage<Project> {

	public ProjectWizardPage() {
		super("ProjectWizardPage");
		setTitle(Messages.NewProject);
		setMessage(Messages.CreatesANewProject);
		setImageDescriptor(ImageType.NEW_WIZ_PROJECT.getDescriptor());
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
