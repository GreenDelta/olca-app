package org.openlca.app.wizards;

import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.core.database.BaseDao;
import org.openlca.core.model.FlowProperty;

public class FlowPropertyWizard extends AbstractWizard<FlowProperty> {

	@Override
	protected String getTitle() {
		return Messages.NewFlowProperty;
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
