package org.openlca.app.newwizards;

import org.openlca.core.application.Messages;
import org.openlca.core.application.db.Database;
import org.openlca.core.database.BaseDao;
import org.openlca.core.model.FlowProperty;

public class FlowPropertyWizard extends AbstractWizard<FlowProperty> {

	@Override
	protected String getTitle() {
		return Messages.FlowProps_WizardTitle;
	}

	@Override
	protected BaseDao<FlowProperty> createDao() {
		return Database.createDao(FlowProperty.class);
	}

	@Override
	protected AbstractWizardPage<FlowProperty> createPage() {
		return new FlowPropertyWizardPage();
	}

}
