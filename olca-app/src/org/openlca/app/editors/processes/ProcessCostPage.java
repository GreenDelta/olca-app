package org.openlca.app.editors.processes;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.Messages;
import org.openlca.app.util.UI;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowType;

public class ProcessCostPage extends FormPage {

	private ProcessEditor editor;

	public ProcessCostPage(ProcessEditor editor) {
		super(editor, "process.CostPage", Messages.ProcessCosts);
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.ProcessCosts);
		FormToolkit toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		for (Exchange e : getOutputProducts()) {
			ProcessCostSection section = new ProcessCostSection(e, editor);
			section.render(toolkit, body);
		}
		form.reflow(true);
	}

	private List<Exchange> getOutputProducts() {
		List<Exchange> list = new ArrayList<>();
		for (Exchange e : editor.getModel().getExchanges()) {
			if (e.isInput())
				continue;
			Flow f = e.getFlow();
			if (f != null && f.getFlowType() == FlowType.PRODUCT_FLOW)
				list.add(e);
		}
		return list;
	}
}
