package org.openlca.core.editors.actor;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.core.application.Messages;
import org.openlca.core.editors.INewModelWizard;
import org.openlca.core.model.Category;
import org.openlca.core.resources.ImageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActorWizard extends Wizard implements INewModelWizard {

	private Logger log = LoggerFactory.getLogger(getClass());
	private Category category;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(Messages.Actors_WizardTitle);
		setDefaultPageImageDescriptor(ImageType.NEW_WIZARD.getDescriptor());
	}

	@Override
	public void setCategory(Category category) {
		this.category = category;
	}

	@Override
	public void addPages() {
		addPage(new ActorWizardPage());
	}

	@Override
	public boolean performFinish() {
		log.trace("finish create actor");

		return false;
	}

}
