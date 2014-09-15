package org.openlca.app.wizards;

import org.openlca.app.Messages;
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
		return Messages.NewActor;
	}

	@Override
	protected AbstractWizardPage<Actor> createPage() {
		return new ActorWizardPage();
	}

}
