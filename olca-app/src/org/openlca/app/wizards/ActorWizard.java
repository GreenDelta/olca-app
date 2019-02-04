package org.openlca.app.wizards;

import java.util.UUID;

import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.core.model.Actor;
import org.openlca.core.model.ModelType;

public class ActorWizard extends AbstractWizard<Actor> {

	@Override
	protected String getTitle() {
		return M.NewActor;
	}

	@Override
	protected AbstractWizardPage<Actor> createPage() {
		return new ActorWizardPage();
	}

	@Override
	protected ModelType getModelType() {
		return ModelType.ACTOR;
	}

	private class ActorWizardPage extends AbstractWizardPage<Actor> {

		public ActorWizardPage() {
			super("ActorWizardPage");
			setTitle(M.NewActor);
			setMessage(M.CreatesANewActor);
			setPageComplete(false);
		}

		@Override
		protected void createContents(Composite container) {
		}

		@Override
		public Actor createModel() {
			Actor actor = new Actor();
			actor.refId = UUID.randomUUID().toString();
			actor.name = getModelName();
			actor.description = getModelDescription();
			return actor;
		}

	}

}
