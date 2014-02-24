package org.openlca.app.editors.processes;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.UI;
import org.openlca.core.model.Process;

class ProcessExchangePage extends ModelPage<Process> {

	private ProcessEditor editor;
	private FormToolkit toolkit;

	ProcessExchangePage(ProcessEditor editor) {
		super(editor, "ProcessExchangePage", Messages.InputOutputPageLabel);
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.Process + ": "
				+ getModel().getName());
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		Section inputSection = UI.section(body, toolkit, Messages.Inputs);
		ExchangeTable.forInputs(inputSection, toolkit, editor);
		Section outputSection = UI.section(body, toolkit, Messages.Outputs);
		ExchangeTable.forOutputs(outputSection, toolkit, editor);
		body.setFocus();
		form.reflow(true);
	}

}
