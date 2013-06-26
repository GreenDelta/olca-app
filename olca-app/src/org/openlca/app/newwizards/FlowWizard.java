package org.openlca.app.newwizards;

import org.openlca.core.application.Messages;
import org.openlca.core.application.db.Database;
import org.openlca.core.database.BaseDao;
import org.openlca.core.model.Flow;

public class FlowWizard extends AbstractWizard<Flow> {

	@Override
	protected BaseDao<Flow> createDao() {
		return Database.createDao(Flow.class);
	}

	@Override
	protected AbstractWizardPage<Flow> createPage() {
		return new FlowWizardPage();
	}

	@Override
	protected String getTitle() {
		return Messages.Flows_WizardTitle;
	}

}
