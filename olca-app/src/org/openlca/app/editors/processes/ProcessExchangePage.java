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
		ExchangeTable inputTable = createTable(body, true);
		ExchangeTable outputTable = createTable(body, false);
		body.setFocus();
		form.reflow(true);
		inputTable.setInitialInput();
		outputTable.setInitialInput();
	}

	private ExchangeTable createTable(Composite body, boolean forInputs) {
		String title = forInputs ? Messages.Inputs : Messages.Outputs;
		Section section = UI.section(body, toolkit, title);
		UI.gridData(section, true, true);
		if (forInputs)
			return ExchangeTable.forInputs(section, toolkit, editor);
		else
			return ExchangeTable.forOutputs(section, toolkit, editor);
	}
}
