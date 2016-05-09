package org.openlca.app.editors.parameters;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.IMessage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.components.UncertaintyDialog;
import org.openlca.app.db.Database;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.UI;
import org.openlca.app.util.UncertaintyLabel;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.model.Parameter;

public class GlobalParameterInfoPage extends ModelPage<Parameter> {

	private ParameterChangeSupport support = new ParameterChangeSupport();
	private List<Parameter> parameters;
	private FormToolkit toolkit;
	private ScrolledForm form;
	private boolean hasErrors;

	GlobalParameterInfoPage(GlobalParameterEditor editor) {
		super(editor, "GlobalParameterInfoPage", M.GeneralInformation);
		support.onEvaluation(this::evalFormulas);
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
			form.getMessageManager().addMessage("invalidFormula",
					M.InvalidFormula + ": " + error, null, IMessage.ERROR);
	}

	boolean hasErrors() {
		return hasErrors;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = UI.formHeader(managedForm);
		updateFormTitle();
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		InfoSection infoSection = new InfoSection(getEditor());
		infoSection.render(body, toolkit);
		createAdditionalInfo(body);
		body.setFocus();
		form.reflow(true);
	}

	@Override
	protected void updateFormTitle() {
		if (form == null)
			return;
		form.setText(M.Parameter + ": " + getModel().getName());
	}

	private void createAdditionalInfo(Composite body) {
		Composite composite = UI.formSection(body, toolkit,
				M.AdditionalInformation);
		UI.formLabel(composite, toolkit, M.Type);
		UI.formLabel(composite, toolkit,
				getModel().isInputParameter() ? M.InputParameter
						: M.DependentParameter);
		if (getModel().isInputParameter())
			createValueSection(composite);
		else
			createFormulaSection(composite);
		UI.formLabel(composite, toolkit, M.Uncertainty);
		Hyperlink link = UI.formLink(composite, toolkit,
				UncertaintyLabel.get(getModel().getUncertainty()));
		link.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				UncertaintyDialog dialog = new UncertaintyDialog(UI.shell(),
						getModel().getUncertainty());
				if (dialog.open() != IDialogConstants.OK_ID)
					return;
				getModel().setUncertainty(dialog.getUncertainty());
				link.setText(UncertaintyLabel.get(getModel().getUncertainty()));
				getEditor().setDirty(true);
			}
		});
	}

	private void createValueSection(Composite parent) {
		createDoubleText(M.Value, "value", parent);
	}

	private void createFormulaSection(Composite parent) {
		Text formulaText = createText(M.Formula, "formula", parent);
		UI.formLabel(parent, toolkit, M.Value);
		Label valueLabel = UI.formLabel(parent, toolkit,
				Double.toString(getModel().getValue()));
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
