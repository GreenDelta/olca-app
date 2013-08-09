package org.openlca.app.editors;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.table.ParameterViewer;
import org.openlca.core.model.Process;

class ProcessParameterPage extends ModelPage<Process> {

	private FormToolkit toolkit;

	ProcessParameterPage(ProcessEditor editor) {
		super(editor, "ProcessParameterPage", Messages.ParametersPageLabel);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.Process + ": "
				+ getModel().getName());
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);

		// TODO: just a demo to show a section for input parameters
		Section iSection = UI.section(body, toolkit, "Input parameters");
		UI.gridData(iSection, true, true);
		Composite iClient = UI.sectionClient(iSection, toolkit);
		new ParameterViewer(iClient);

		Section section = UI.section(body, toolkit, "Dependent parameters");
		UI.gridData(section, true, true);
		Composite client = UI.sectionClient(section, toolkit);

		ParameterViewer parameterViewer = new ParameterViewer(client);
		getBinding().on(getModel(), "parameters", parameterViewer);
		parameterViewer.bindTo(section);

		body.setFocus();
		form.reflow(true);
	}

}
