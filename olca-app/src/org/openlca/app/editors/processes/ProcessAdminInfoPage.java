/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
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

class ProcessAdminInfoPage extends ModelPage<Process> {

	private FormToolkit toolkit;

	ProcessAdminInfoPage(ProcessEditor editor) {
		super(editor, "ProcessInfoPage", Messages.AdminInfoPageLabel);
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
				Messages.AdminInfoPageLabel);
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
		createMultiText(Messages.Version, "documentation.version", composite);
		createReadOnly(Messages.CreationDate, "documentation.creationDate",
				composite);
		createReadOnly(Messages.LastChange, "documentation.lastChange",
				composite);
		createCheckBox(Messages.Copyright, "documentation.copyright", composite);
	}

}
