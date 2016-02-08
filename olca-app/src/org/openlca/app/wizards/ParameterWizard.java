package org.openlca.app.wizards;

import java.util.UUID;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.util.UI;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.model.ModelType;
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
		return M.NewParameter;
	}

	@Override
	protected AbstractWizardPage<Parameter> createPage() {
		return new ParameterWizardPage();
	}
	
	@Override
	protected ModelType getModelType() {
		return ModelType.PARAMETER;
	}


	private class ParameterWizardPage extends AbstractWizardPage<Parameter> {

		public ParameterWizardPage() {
			super("ParameterWizardPage");
			setTitle(M.NewParameter);
			setMessage(M.CreatesANewParameter);
			setPageComplete(false);
		}

		@Override
		protected void createContents(Composite container) {
			UI.formLabel(container, M.Type);
			Composite formulaValueSwitcher = UI.formComposite(container);
			UI.gridLayout(formulaValueSwitcher, 4);
			inputButton = UI.formRadio(formulaValueSwitcher, M.InputParameter);
			dependentButton = UI.formRadio(formulaValueSwitcher, M.DependentParameter);
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
				setErrorMessage(M.ParameterWithSameNameExists);
				setPageComplete(false);
				return;
			}
			if (!Parameter.isValidName(name)) {
				setErrorMessage(name + " " + M.IsNotValidParameterName);
				setPageComplete(false);
				return;
			}
			setErrorMessage(null);
			setPageComplete(true);
		}

	}

}