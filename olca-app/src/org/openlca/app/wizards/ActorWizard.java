package org.openlca.app.wizards;

import java.util.UUID;

import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.core.database.BaseDao;
import org.openlca.core.model.Actor;

public class ActorWizard extends AbstractWizard<Actor> {

	@Override
	protected BaseDao<Actor> createDao() {
		return Database.createDao(Actor.class);
	}

	@Override
	protected String getTitle() {
		return M.NewActor;
	}

	@Override
	protected AbstractWizardPage<Actor> createPage() {
		return new ActorWizardPage();
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
			actor.setRefId(UUID.randomUUID().toString());
			actor.setName(getModelName());
			actor.setDescription(getModelDescription());
			return actor;
		}

	}

}
