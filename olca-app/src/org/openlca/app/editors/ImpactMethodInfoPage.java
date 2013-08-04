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
import org.openlca.app.util.UI;
import org.openlca.app.viewers.table.ImpactCategoryViewer;
import org.openlca.core.model.ImpactMethod;

public class ImpactMethodInfoPage extends ModelPage<ImpactMethod> {

	private FormToolkit toolkit;

	public ImpactMethodInfoPage(ImpactMethodEditor editor) {
		super(editor, "ImpactMethodInfoPage",
				Messages.GeneralInformation);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.ImpactMethod
				+ ": " + getModel().getName());
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		InfoSection infoSection = new InfoSection(getModel(), getBinding());
		infoSection.render(body, toolkit);
		createImpactCategoryViewer(body);
		body.setFocus();
		form.reflow(true);
	}

	private void createImpactCategoryViewer(Composite body) {
		Section section = UI.section(body, toolkit,
				Messages.ImpactCategories);
		UI.gridData(section, true, true);
		Composite client = UI.sectionClient(section, toolkit);
		ImpactCategoryViewer viewer = new ImpactCategoryViewer(client);
		getBinding().on(getModel(), "impactCategories", viewer);
		viewer.bindTo(section);
	}

}
