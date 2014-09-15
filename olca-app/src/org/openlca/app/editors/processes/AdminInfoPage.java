package org.openlca.app.editors.processes;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.Messages;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.UI;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;

class AdminInfoPage extends ModelPage<Process> {

	private FormToolkit toolkit;

	AdminInfoPage(ProcessEditor editor) {
		super(editor, "ProcessInfoPage", Messages.AdministrativeInformation);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.Process + ": "
				+ getModel().getName());
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		createAdminInfoSection(body);
		body.setFocus();
		form.reflow(true);
	}

	private void createAdminInfoSection(Composite parent) {
		Composite composite = UI.formSection(parent, toolkit,
				Messages.AdministrativeInformation);
		createMultiText(Messages.IntendedApplication,
				"documentation.intendedApplication", composite);
		createDropComponent(Messages.DataSetOwner,
				"documentation.dataSetOwner", ModelType.ACTOR, composite);
		createDropComponent(Messages.DataGenerator,
				"documentation.dataGenerator", ModelType.ACTOR, composite);
		createDropComponent(Messages.DataDocumentor,
				"documentation.dataDocumentor", ModelType.ACTOR, composite);
		createDropComponent(Messages.Publication, "documentation.publication",
				ModelType.SOURCE, composite);
		createMultiText(Messages.AccessAndUseRestrictions,
				"documentation.restrictions", composite);
		createMultiText(Messages.Project, "documentation.project", composite);
		createReadOnly(Messages.CreationDate, "documentation.creationDate",
				composite);
		createCheckBox(Messages.Copyright, "documentation.copyright", composite);
	}

}
