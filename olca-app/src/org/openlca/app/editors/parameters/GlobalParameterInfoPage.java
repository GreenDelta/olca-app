package org.openlca.app.editors.parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.IMessage;
import org.eclipse.ui.forms.widgets.FormToolkit;
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
import org.openlca.core.model.UncertaintyType;

class GlobalParameterInfoPage extends ModelPage<Parameter> {

	private final ParameterChangeSupport support = new ParameterChangeSupport();
	private final List<Parameter> otherGlobals;
	private FormToolkit toolkit;
	private ScrolledForm form;
	private boolean hasErrors;

	GlobalParameterInfoPage(GlobalParameterEditor editor) {
		super(editor, "GlobalParameterInfoPage", M.GeneralInformation);
		support.onEvaluation(this::evalFormulas);
		// collect the other global parameters for fast formula evaluation
		otherGlobals = new ParameterDao(Database.get())
				.getGlobalParameters()
				.stream()
				.filter(p -> !Objects.equals(p, getModel()))
				.collect(Collectors.toList());
	}

	private void evalFormulas() {
		form.getMessageManager().removeAllMessages();
		var params = new ArrayList<>(otherGlobals);
		params.add(getModel());
		var errors = Formulas.eval(params);
		hasErrors = errors.size() > 0;
		for (String error : errors)
			form.getMessageManager()
					.addMessage("invalidFormula",
							M.InvalidFormula + ": " + error, null, IMessage.ERROR);
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
		if (getModel().isInputParameter) {
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
		var link = UI.formLink(
				comp, toolkit, Uncertainty.string(getModel().uncertainty));
		Controls.onClick(link, e -> {
			var param = getModel();
			var u = UncertaintyDialog.open(param.uncertainty)
					.orElse(null);
			if (u == null)
				return;
			var isNone = u.distributionType == null
					|| u.distributionType == UncertaintyType.NONE;
			param.uncertainty = isNone ? null : u;
			link.setText(Uncertainty.string(param.uncertainty));
			link.getParent().layout();
			getEditor().setDirty(true);
		});
		UI.filler(comp, toolkit);
	}

	private void forDependentParameter(Composite comp) {
		Text text = text(comp, M.Formula, "formula");
		UI.formLabel(comp, toolkit, M.Value);
		Label label = UI.formLabel(comp, toolkit, Double.toString(getModel().value));
		text.addModifyListener(e -> {
			support.evaluate();
			label.setText(Double.toString(getModel().value));
			getEditor().setDirty(true);
			comp.layout();
		});
		ParameterProposals.on(text);
		support.evaluate();
		label.setText(Double.toString(getModel().value));
	}
}
