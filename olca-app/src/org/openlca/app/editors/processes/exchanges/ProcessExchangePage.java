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

	final ProcessEditor editor;
	FormToolkit toolkit;

	private ScrolledForm form;
	private ExchangeTable inputTable;
	private ExchangeTable outputTable;

	public ProcessExchangePage(ProcessEditor editor) {
		super(editor, "ProcessExchangePage", M.InputsOutputs);
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		form = UI.formHeader(this);
		toolkit = mform.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		SashForm sash = new SashForm(body, SWT.VERTICAL);
		UI.gridData(sash, true, true);
		toolkit.adapt(sash);
		inputTable = createTable(sash, true);
		outputTable = createTable(sash, false);
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

	private void sortExchanges() {
		List<Exchange> exchanges = editor.getModel().exchanges;
		exchanges.sort((e1, e2) -> {
			if (e1.flow == null || e2.flow == null)
				return 0;
			int c = Strings.compare(e1.flow.name, e2.flow.name);
			if (c != 0)
				return c;
			String c1 = CategoryPath.getShort(e1.flow.category);
			String c2 = CategoryPath.getShort(e2.flow.category);
			return Strings.compare(c1, c2);
		});
	}

	private ExchangeTable createTable(Composite body, boolean forInputs) {
		String title = forInputs ? M.Inputs : M.Outputs;
		Section section = UI.section(body, toolkit, title);
		UI.gridData(section, true, true);
		if (forInputs)
			return ExchangeTable.forInputs(section, this);
		else
			return ExchangeTable.forOutputs(section, this);
	}

	void refreshTables() {
		if (inputTable != null && inputTable.viewer != null)
			inputTable.viewer.refresh();
		if (outputTable != null && outputTable.viewer != null)
			outputTable.viewer.refresh();
	}
}
