package org.openlca.core.editors.actor;

import java.util.UUID;

import org.eclipse.swt.widgets.Composite;
import org.openlca.core.application.Messages;
import org.openlca.core.application.views.navigator.ModelWizardPage;
import org.openlca.core.model.Actor;
import org.openlca.core.resources.ImageType;

public class ActorWizardPage extends ModelWizardPage {

	/**
	 * Creates a new actor wizard page
	 */
	public ActorWizardPage() {
		super("ActorWizardPage");
		setTitle(Messages.Actors_WizardTitle);
		setMessage(Messages.Actors_WizardMessage);
		setImageDescriptor(ImageType.NEW_WIZ_ACTOR.getDescriptor());
		setPageComplete(false);
	}

	@Override
	protected void createContents(Composite container) {
	}

	public Actor getActor() {
		Actor actor = new Actor();
		actor.setId(UUID.randomUUID().toString());
		actor.setName(getComponentName());
		actor.setDescription(getComponentDescription());
		return actor;
	}

}
