package org.openlca.app.newwizards;

import java.util.Calendar;
import java.util.UUID;

import org.eclipse.swt.widgets.Composite;
import org.openlca.core.application.Messages;
import org.openlca.core.model.Project;
import org.openlca.core.resources.ImageType;

class ProjectWizardPage extends AbstractWizardPage<Project> {

	public ProjectWizardPage() {
		super("ProjectWizardPage");
		setTitle(Messages.Projects_WizardTitle);
		setMessage(Messages.Projects_WizardMessage);
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
