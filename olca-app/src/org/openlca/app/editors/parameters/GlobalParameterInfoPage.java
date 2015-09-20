package org.openlca.app.editors.parameters;

import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.IMessage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.UI;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.model.Parameter;

public class GlobalParameterInfoPage extends ModelPage<Parameter> {

	private ParameterChangeSupport support = new ParameterChangeSupport();
	private List<Parameter> parameters;
	private FormToolkit toolkit;
	private ScrolledForm form;
	private boolean hasErrors;

	GlobalParameterInfoPage(GlobalParameterEditor editor) {
		super(editor, "GlobalParameterInfoPage", Messages.GeneralInformation);
		support.doEvaluation(this::evalFormulas);
		parameters = new ParameterDao(Database.get()).getGlobalParameters();
		for (int i = 0; i < parameters.size(); i++)
			if (parameters.get(i).getName().equals(getModel().getName()))
				parameters.remove(i);
		parameters.add(getModel());
	}

	private void evalFormulas() {
		form.getMessageManager().removeAllMessages();
		List<String> errors = Formulas.eval(parameters);
		hasErrors = errors.size() > 0;
		for (String error : errors)
			form.getMessageManager().addMessage("invalidFormula", "#Invalid formula: " + error, null, IMessage.ERROR);
	}

	boolean hasErrors() {
		return hasErrors;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = UI.formHeader(managedForm, Messages.Parameter + ": " + getModel().getName());
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		InfoSection infoSection = new InfoSection(getEditor());
		infoSection.render(body, toolkit);
		createAdditionalInfo(body);
		body.setFocus();
		form.reflow(true);
	}

	private void createAdditionalInfo(Composite body) {
		Composite composite = UI.formSection(body, toolkit, Messages.AdditionalInformation);
		UI.formLabel(composite, toolkit, Messages.Type);
		UI.formLabel(composite, toolkit, getModel().isInputParameter() ? "#Input parameter" : "#Dependent parameter");
		if (getModel().isInputParameter())
			createValueSection(composite);
		else
			createFormulaSection(composite);
	}

	private void createValueSection(Composite parent) {
		createDoubleText(Messages.Value, "value", parent);
	}

	private void createFormulaSection(Composite parent) {
		Text formulaText = createText(Messages.Formula, "formula", parent);
		UI.formLabel(parent, toolkit, Messages.Value);
		Label valueLabel = UI.formLabel(parent, toolkit, Double.toString(getModel().getValue()));
		formulaText.addModifyListener((event) -> {
			support.evaluate();
			valueLabel.setText(Double.toString(getModel().getValue()));
			getEditor().setDirty(true);
			parent.layout();
		});
		support.evaluate();
		valueLabel.setText(Double.toString(getModel().getValue()));
	}

}
