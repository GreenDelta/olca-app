package org.openlca.app.processes;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.db.Database;
import org.openlca.app.util.UI;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;

public class ProcessCostPage extends FormPage {

	private Process process;
	private ProcessEditor editor;

	public ProcessCostPage(ProcessEditor editor) {
		super(editor, "process.CostPage", "Costs");
		process = (Process) editor.getModelComponent();
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		toolkit.getHyperlinkGroup().setHyperlinkUnderlineMode(
				HyperlinkSettings.UNDERLINE_HOVER);
		toolkit.decorateFormHeading(form.getForm());
		form.setText("Process costs");
		Composite body = UI.formBody(form, toolkit);

		for (Exchange e : getOutputProducts()) {
			ProcessCostSection section = new ProcessCostSection(e,
					Database.get(), editor);
			section.render(toolkit, body);
		}

		form.reflow(true);
	}

	private List<Exchange> getOutputProducts() {
		List<Exchange> list = new ArrayList<>();
		for (Exchange e : process.getExchanges()) {
			if (e.isInput())
				continue;
			Flow f = e.getFlow();
			if (f != null && f.getFlowType() == FlowType.PRODUCT_FLOW)
				list.add(e);
		}
		return list;
	}
}
