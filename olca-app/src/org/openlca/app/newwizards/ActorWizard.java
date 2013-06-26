package org.openlca.app.newwizards;

import org.openlca.core.application.Messages;
import org.openlca.core.application.db.Database;
import org.openlca.core.database.BaseDao;
import org.openlca.core.model.Actor;

public class ActorWizard extends AbstractWizard<Actor> {

	@Override
	protected BaseDao<Actor> createDao() {
		return Database.createDao(Actor.class);
	}

	@Override
	protected String getTitle() {
		return Messages.Actors_WizardTitle;
	}

	@Override
	protected AbstractWizardPage<Actor> createPage() {
		return new ActorWizardPage();
	}

}
