package org.openlca.app.editors.parameters;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.IMessage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.components.ParameterProposals;
import org.openlca.app.components.UncertaintyDialog;
import org.openlca.app.db.Database;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.Uncertainty;

class GlobalParameterInfoPage extends ModelPage<Parameter> {

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
			form.getMessageManager()
					.addMessage("invalidFormula", M.InvalidFormula + ": " + error, null, IMessage.ERROR);
	}

	boolean hasErrors() {
		return hasErrors;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = UI.formHeader(this);
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		InfoSection infoSection = new InfoSection(getEditor());
		infoSection.render(body, toolkit);
		createAdditionalInfo(body);
		body.setFocus();
		form.reflow(true);
	}

	private void createAdditionalInfo(Composite body) {
		Composite comp = UI.formSection(body, toolkit, M.AdditionalInformation, 3);
		UI.formLabel(comp, toolkit, M.Type);
		if (getModel().isInputParameter()) {
			UI.formLabel(comp, toolkit, M.InputParameter);
			UI.filler(comp, toolkit);
			forInputParameter(comp);
		} else {
			UI.formLabel(comp, toolkit, M.DependentParameter);
			UI.filler(comp, toolkit);
			forDependentParameter(comp);
		}
	}

	private void forInputParameter(Composite comp) {
		doubleText(comp, M.Value, "value");
		UI.formLabel(comp, toolkit, M.Uncertainty);
		Hyperlink link = UI.formLink(comp, toolkit, Uncertainty.string(getModel().getUncertainty()));
		Controls.onClick(link, e -> {
			UncertaintyDialog dialog = new UncertaintyDialog(UI.shell(), getModel().getUncertainty());
			if (dialog.open() != IDialogConstants.OK_ID)
				return;
			getModel().setUncertainty(dialog.getUncertainty());
			link.setText(Uncertainty.string(getModel().getUncertainty()));
			getEditor().setDirty(true);
		});
		UI.filler(comp, toolkit);
	}

	private void forDependentParameter(Composite comp) {
		Text text = text(comp, M.Formula, "formula");
		UI.formLabel(comp, toolkit, M.Value);
		Label label = UI.formLabel(comp, toolkit, Double.toString(getModel().getValue()));
		text.addModifyListener(e -> {
			support.evaluate();
			label.setText(Double.toString(getModel().getValue()));
			getEditor().setDirty(true);
			comp.layout();
		});
		ParameterProposals.on(text);
		support.evaluate();
		label.setText(Double.toString(getModel().getValue()));
	}
}
