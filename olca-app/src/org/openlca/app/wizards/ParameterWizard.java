package org.openlca.app.wizards;

import java.util.UUID;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.UI;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.model.Parameter;

public class ParameterWizard extends AbstractWizard<Parameter> {

	private Button inputButton;
	private Button dependentButton;

	@Override
	protected ParameterDao createDao() {
		return new ParameterDao(Database.get());
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

		@Override
		protected void checkInput() {
			super.checkInput();
			if (!isPageComplete())
				return;
			String name = getModelName();
			if (createDao().existsGlobal(name)) {
				setErrorMessage("#A parameter with the same name already exists");
				setPageComplete(false);
				return;
			}
			if (!Parameter.isValidName(name)) {
				setErrorMessage(name + " " + Messages.IsNotValidParameterName);
				setPageComplete(false);
				return;
			}
			setErrorMessage(null);
			setPageComplete(true);
		}

	}

}