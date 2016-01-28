package org.openlca.app.editors.processes;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.UI;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;

class AdminInfoPage extends ModelPage<Process> {

	private FormToolkit toolkit;
	private ScrolledForm form;

	AdminInfoPage(ProcessEditor editor) {
		super(editor, "ProcessInfoPage", M.AdministrativeInformation);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = UI.formHeader(managedForm);
		updateFormTitle();
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		createAdminInfoSection(body);
		body.setFocus();
		form.reflow(true);
	}

	@Override
	protected void updateFormTitle() {
		if (form == null)
			return;
		form.setText(M.Process + ": " + getModel().getName());
	}

	private void createAdminInfoSection(Composite parent) {
		Composite composite = UI.formSection(parent, toolkit,
				M.AdministrativeInformation);
		createMultiText(M.IntendedApplication,
				"documentation.intendedApplication", composite);
		createDropComponent(M.DataSetOwner,
				"documentation.dataSetOwner", ModelType.ACTOR, composite);
		createDropComponent(M.DataGenerator,
				"documentation.dataGenerator", ModelType.ACTOR, composite);
		createDropComponent(M.DataDocumentor,
				"documentation.dataDocumentor", ModelType.ACTOR, composite);
		createDropComponent(M.Publication, "documentation.publication",
				ModelType.SOURCE, composite);
		createMultiText(M.AccessAndUseRestrictions,
				"documentation.restrictions", composite);
		createMultiText(M.Project, "documentation.project", composite);
		createReadOnly(M.CreationDate, "documentation.creationDate",
				composite);
		createCheckBox(M.Copyright, "documentation.copyright", composite);
	}

}
