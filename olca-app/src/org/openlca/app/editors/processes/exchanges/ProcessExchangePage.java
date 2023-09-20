package org.openlca.app.editors.processes.exchanges;

import java.util.Objects;

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
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;
import org.openlca.io.CategoryPath;
import org.openlca.util.Strings;

public class ProcessExchangePage extends ModelPage<Process> {

	final ProcessEditor editor;
	FormToolkit toolkit;

	private ExchangeTable inputTable;
	private ExchangeTable outputTable;

	public ProcessExchangePage(ProcessEditor editor) {
		super(editor, "ProcessExchangePage", M.InputsOutputs);
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.header(this);
		toolkit = mform.getToolkit();
		Composite body = UI.body(form, toolkit);
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
		var process = editor.getModel();
		var exchanges = process.exchanges;
		exchanges.sort((e1, e2) -> {

			// quant. reference first
			if (Objects.equals(process.quantitativeReference, e1))
				return -1;
			if (Objects.equals(process.quantitativeReference, e2))
				return 1;

			// null checks for flows
			if (e1.flow == null && e2.flow == null)
				return 0;
			if (e1.flow == null)
				return -1;
			if (e2.flow == null)
				return 1;

			// flow type
			var t1 = e1.flow.flowType;
			var t2 = e2.flow.flowType;
			if ( t1 != null && t2 != null && t1 != t2) {
				if (t1 == FlowType.PRODUCT_FLOW)
					return -1;
				if (t2 == FlowType.PRODUCT_FLOW)
					return 1;
				if (t1 == FlowType.WASTE_FLOW)
					return -1;
				if (t2 == FlowType.WASTE_FLOW)
					return  1;
			}

			// name or category
			int c = Strings.compare(e1.flow.name, e2.flow.name);
			if (c != 0)
				return c;
			var c1 = CategoryPath.getShort(e1.flow.category);
			var c2 = CategoryPath.getShort(e2.flow.category);
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
