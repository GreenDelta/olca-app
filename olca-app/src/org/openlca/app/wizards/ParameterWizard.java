package org.openlca.app.wizards;

import java.util.List;
import java.util.UUID;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.M;
import org.openlca.app.components.ParameterProposals;
import org.openlca.app.db.Database;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Parameter;
import org.openlca.expressions.FormulaInterpreter;
import org.openlca.util.Parameters;
import org.python.google.common.base.Strings;

public class ParameterWizard extends AbstractWizard<Parameter> {

	private Button inputButton;
	private Button dependentButton;
	private TwoControlStack formulaLabels;
	private Text formulaText;

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

		private final List<Parameter> parameters;
		private double lastCalculated = 0d;
		private boolean contentAssistEnabled = false;

		public ParameterWizardPage() {
			super("ParameterWizardPage");
			setTitle(M.NewParameter);
			setMessage(M.CreatesANewParameter);
			setPageComplete(false);
			parameters = new ParameterDao(Database.get()).getGlobalParameters();
		}

		@Override
		protected void modelWidgets(Composite parent) {
			UI.formLabel(parent, M.Type);
			var comp = UI.formComposite(parent);
			UI.gridLayout(comp, 4);
			inputButton = UI.formRadio(comp, M.InputParameter);
			dependentButton = UI.formRadio(comp, M.DependentParameter);
			inputButton.setSelection(true);
			Controls.onSelect(inputButton, (e) -> {
				formulaLabels.switchControls();
				checkInput();
				if (!inputButton.getSelection()
						&& !contentAssistEnabled) {
					ParameterProposals.on(formulaText);
					contentAssistEnabled = true;
				}
			});
			createFormulaAndAmount(parent);
		}

		private void createFormulaAndAmount(Composite container) {
			formulaLabels = new TwoControlStack(container);
			UI.gridData(formulaLabels, true, false);
			formulaLabels.initControls(UI.formLabel(formulaLabels, M.Amount),
					UI.formLabel(formulaLabels, M.Formula));
			formulaText = UI.formText(container, SWT.NONE);
			formulaText.addModifyListener((e) -> checkInput());
		}

		@Override
		public Parameter createModel() {
			Parameter p = new Parameter();
			p.refId = UUID.randomUUID().toString();
			p.name = getModelName();
			p.description = getModelDescription();
			p.isInputParameter = !dependentButton.getSelection();
			if (Strings.isNullOrEmpty(formulaText.getText()))
				return p;
			if (p.isInputParameter) {
				try {
					p.value = Double.parseDouble(formulaText.getText());
				} catch (NumberFormatException e) {
					p.value = 0d;
				}
			} else {
				p.formula = formulaText.getText();
				p.value = lastCalculated;
			}
			return p;
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
			if (!Parameters.isValidName(name)) {
				setErrorMessage(name + " " + M.IsNotValidParameterName);
				setPageComplete(false);
				return;
			}
			String formula = formulaText.getText();
			if (!formulaIsValid(formula))
				return;
			setErrorMessage(null);
			setPageComplete(true);
		}

		private boolean formulaIsValid(String formula) {
			if (Strings.isNullOrEmpty(formula))
				return true; // formula is optional
			if (inputButton.getSelection()) {
				try {
					Double.parseDouble(formula);
					return true;
				} catch (NumberFormatException e) {
					setErrorMessage(formula + " " + M.IsNotValidNumber);
					setPageComplete(false);
					return false;
				}
			}

			var interpreter = new FormulaInterpreter();
			for (var param : this.parameters) {
				if (param.isInputParameter) {
					interpreter.bind(param.name, param.value);
				} else {
					interpreter.bind(param.name, param.formula);
				}
			}
			try {
				lastCalculated = interpreter.eval(formulaText.getText());
				return true;
			} catch (Exception e) {
				lastCalculated = 0;
				setErrorMessage(formula + " " + M.IsInvalidFormula);
				setPageComplete(false);
				return false;
			}
		}
	}

	private static class TwoControlStack extends Composite {

		private Control first;
		private Control second;
		private boolean firstActive;

		public TwoControlStack(Composite parent) {
			super(parent, SWT.NONE);
			setLayout(new StackLayout());
		}

		public void initControls(Control first, Control second) {
			this.first = first;
			this.second = second;
			switchControls();
		}

		public void switchControls() {
			if (firstActive) {
				getLayout().topControl = second;
				firstActive = false;
			} else {
				getLayout().topControl = first;
				firstActive = true;
			}
			layout();
		}

		@Override
		public StackLayout getLayout() {
			return (StackLayout) super.getLayout();
		}

		@Override
		public void setLayout(Layout layout) {
			if (!(layout instanceof StackLayout))
				throw new UnsupportedOperationException(
						"Only supported for StackLayout");
			super.setLayout(layout);
		}

	}

}
