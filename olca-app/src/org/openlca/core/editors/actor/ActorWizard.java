package org.openlca.core.editors.actor;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.core.application.App;
import org.openlca.core.application.Messages;
import org.openlca.core.application.db.Database;
import org.openlca.core.application.navigation.Navigator;
import org.openlca.core.editors.INewModelWizard;
import org.openlca.core.model.Actor;
import org.openlca.core.model.Category;
import org.openlca.core.resources.ImageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActorWizard extends Wizard implements INewModelWizard {

	private Logger log = LoggerFactory.getLogger(getClass());
	private Category category;
	private ActorWizardPage page;

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
		this.page = new ActorWizardPage();
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		log.trace("finish create actor");
		try {
			Actor actor = page.getActor();
			actor.setCategory(category);
			Database.createDao(Actor.class).insert(actor);
			App.openEditor(actor);
			Navigator.refresh();
			return true;
		} catch (Exception e) {
			log.error("failed to create actor", e);
			return false;
		}
	}

}
