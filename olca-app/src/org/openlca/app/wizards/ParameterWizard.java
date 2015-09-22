package org.openlca.app.wizards;

import java.util.UUID;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.UI;
import org.openlca.core.database.BaseDao;
import org.openlca.core.model.Parameter;

public class ParameterWizard extends AbstractWizard<Parameter> {

	private Button inputButton;
	private Button dependentButton;

	@Override
	protected BaseDao<Parameter> createDao() {
		return Database.createDao(Parameter.class);
	}

	@Override
	protected String getTitle() {
		return Messages.NewParameter;
	}

	@Override
	protected AbstractWizardPage<Parameter> createPage() {
		return new ParameterWizardPage();
	}

	private class ParameterWizardPage extends AbstractWizardPage<Parameter> {

		public ParameterWizardPage() {
			super("ParameterWizardPage");
			setTitle(Messages.NewParameter);
			setMessage(Messages.CreatesANewParameter);
			// TODO change icon
			setImageDescriptor(ImageType.NEW_WIZ_ACTOR.getDescriptor());
			setPageComplete(false);
		}

		@Override
		protected void createContents(Composite container) {
			UI.formLabel(container, "#Type");
			Composite formulaValueSwitcher = UI.formComposite(container);
			UI.gridLayout(formulaValueSwitcher, 4);
			inputButton = UI.formRadio(formulaValueSwitcher, "#Input parameter");
			dependentButton = UI.formRadio(formulaValueSwitcher, "#Dependent parameter");
			inputButton.setSelection(true);
		}

		@Override
		public Parameter createModel() {
			Parameter parameter = new Parameter();
			parameter.setRefId(UUID.randomUUID().toString());
			parameter.setName(getModelName());
			parameter.setDescription(getModelDescription());
			parameter.setInputParameter(!dependentButton.getSelection());
			return parameter;
		}

	}

}