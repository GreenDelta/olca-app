package org.openlca.app.editors.processes;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.UI;
import org.openlca.core.model.Process;

class AdminInfoPage extends ModelPage<Process> {

	AdminInfoPage(ProcessEditor editor) {
		super(editor, "ProcessInfoPage", M.AdministrativeInformation);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.formHeader(this);
		var tk = mForm.getToolkit();
		var body = UI.formBody(form, tk);
		createAdminInfoSection(body, tk);
		body.setFocus();
		form.reflow(true);
	}

	private void createAdminInfoSection(Composite parent, FormToolkit tk) {
		var comp = UI.formSection(parent, tk, M.AdministrativeInformation, 3);
		multiText(comp, M.IntendedApplication, "documentation.intendedApplication");
		modelLink(comp, M.DataSetOwner, "documentation.dataSetOwner");
		modelLink(comp, M.DataGenerator, "documentation.dataGenerator");
		modelLink(comp, M.DataDocumentor, "documentation.dataDocumentor");
		modelLink(comp, M.Publication, "documentation.publication");
		multiText(comp, M.AccessAndUseRestrictions, "documentation.restrictions");
		multiText(comp, M.Project, "documentation.project", 40);
		readOnly(comp, M.CreationDate, "documentation.creationDate");
		checkBox(comp, M.Copyright, "documentation.copyright");
	}

}
