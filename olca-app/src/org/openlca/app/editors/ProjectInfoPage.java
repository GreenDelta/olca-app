/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.editors;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.table.ProductSystemViewer;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Project;

class ProjectInfoPage extends ModelPage<Project> {

	private FormToolkit toolkit;

	ProjectInfoPage(ProjectEditor editor) {
		super(editor, "ProjectInfoPage", Messages.GeneralInformation);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.Project + ": "
				+ getModel().getName());
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		InfoSection infoSection = new InfoSection(getModel(), getBinding());
		infoSection.render(body, toolkit);

		createGoalAndScopeSection(body);
		createTimeInfoSection(body);
		createProductSystemSection(body);

		body.setFocus();
		form.reflow(true);
	}

	private void createGoalAndScopeSection(Composite parent) {
		Composite composite = UI.formSection(parent, toolkit,
				Messages.GoalAndScopeInfoSectionLabel);

		createMultiText(Messages.Goal, "goal", composite);
		createMultiText(Messages.FunctionalUnit, "functionalUnit", composite);
	}

	private void createTimeInfoSection(Composite parent) {
		Composite composite = UI.formSection(parent, toolkit,
				Messages.ProjectInfoSectionLabel);

		createReadOnly(Messages.CreationDate, "creationDate", composite);
		createReadOnly(Messages.LastModificationDate, "lastModificationDate",
				composite);
		createDropComponent(Messages.Author, "author", ModelType.ACTOR,
				composite);
	}

	private void createProductSystemSection(Composite parent) {
		Section section = UI.section(parent, toolkit,
				Messages.ProductSystemsInfoSectionLabel);
		UI.gridData(section, true, true);
		Composite client = UI.sectionClient(section, toolkit);

		ProductSystemViewer viewer = new ProductSystemViewer(client,
				Database.getCache());
		viewer.bindTo(section);
		getBinding().on(getModel(), "productSystems", viewer);
	}

}
