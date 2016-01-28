package org.openlca.app.editors.processes.exchanges;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.util.UI;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Process;
import org.openlca.io.CategoryPath;
import org.openlca.util.Strings;

public class ProcessExchangePage extends ModelPage<Process> {

	private ProcessEditor editor;
	private FormToolkit toolkit;
	private ScrolledForm form;

	public ProcessExchangePage(ProcessEditor editor) {
		super(editor, "ProcessExchangePage", M.InputsOutputs);
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = UI.formHeader(managedForm);
		updateFormTitle();
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		SashForm sash = new SashForm(body, SWT.VERTICAL);
		UI.gridData(sash, true, true);
		toolkit.adapt(sash);
		ExchangeTable inputTable = createTable(sash, true);
		ExchangeTable outputTable = createTable(sash, false);
		body.setFocus();
		form.reflow(true);
		sortExchanges();
		inputTable.setInput(getModel());
		outputTable.setInput(getModel());
		editor.onSaved(() -> {
			inputTable.setInput(getModel());
			outputTable.setInput(getModel());
		});
	}

	@Override
	protected void updateFormTitle() {
		if (form == null)
			return;
		form.setText(M.Process + ": " + getModel().getName());
	}

	private void sortExchanges() {
		List<Exchange> exchanges = editor.getModel().getExchanges();
		exchanges.sort((e1, e2) -> {
			if (e1.getFlow() == null || e2.getFlow() == null)
				return 0;
			int c = Strings.compare(e1.getFlow().getName(), e2.getFlow()
					.getName());
			if (c != 0)
				return c;
			String c1 = CategoryPath.getShort(e1.getFlow().getCategory());
			String c2 = CategoryPath.getShort(e2.getFlow().getCategory());
			return Strings.compare(c1, c2);
		});
	}

	private ExchangeTable createTable(Composite body, boolean forInputs) {
		String title = forInputs ? M.Inputs : M.Outputs;
		Section section = UI.section(body, toolkit, title);
		UI.gridData(section, true, true);
		if (forInputs)
			return ExchangeTable.forInputs(section, toolkit, editor);
		else
			return ExchangeTable.forOutputs(section, toolkit, editor);
	}
}
