package org.openlca.app.editors.processes;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.UI;
import org.openlca.core.model.Process;

class AdminInfoPage extends ModelPage<Process> {

	private FormToolkit toolkit;
	private ScrolledForm form;

	AdminInfoPage(ProcessEditor editor) {
		super(editor, "ProcessInfoPage", M.AdministrativeInformation);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = UI.formHeader(this);
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		createAdminInfoSection(body);
		body.setFocus();
		form.reflow(true);
	}

	private void createAdminInfoSection(Composite parent) {
		Composite composite = UI.formSection(parent, toolkit, M.AdministrativeInformation, 3);
		multiText(composite, M.IntendedApplication, "documentation.intendedApplication");
		dropComponent(composite, M.DataSetOwner, "documentation.dataSetOwner");
		dropComponent(composite, M.DataGenerator, "documentation.dataGenerator");
		dropComponent(composite, M.DataDocumentor, "documentation.dataDocumentor");
		dropComponent(composite, M.Publication, "documentation.publication");
		multiText(composite, M.AccessAndUseRestrictions, "documentation.restrictions");
		multiText(composite, M.Project, "documentation.project", 40);
		readOnly(composite, M.CreationDate, "documentation.creationDate");
		checkBox(composite, M.Copyright, "documentation.copyright");
	}

}
