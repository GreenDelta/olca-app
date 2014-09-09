package org.openlca.app.wizards;

import java.util.UUID;

import org.eclipse.swt.widgets.Composite;
import org.openlca.app.Messages;
import org.openlca.app.rcp.ImageType;
import org.openlca.core.model.Actor;

class ActorWizardPage extends AbstractWizardPage<Actor> {

	public ActorWizardPage() {
		super("ActorWizardPage");
		setTitle(Messages.NewActor);
		setMessage(Messages.CreatesANewActor);
		setImageDescriptor(ImageType.NEW_WIZ_ACTOR.getDescriptor());
		setPageComplete(false);
	}

	@Override
	protected void createContents(Composite container) {
	}

	@Override
	public Actor createModel() {
		Actor actor = new Actor();
		actor.setRefId(UUID.randomUUID().toString());
		actor.setName(getModelName());
		actor.setDescription(getModelDescription());
		return actor;
	}

}
