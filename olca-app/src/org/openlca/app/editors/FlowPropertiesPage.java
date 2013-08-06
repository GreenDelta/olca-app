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
import org.openlca.app.viewers.table.FlowPropertyFactorViewer;
import org.openlca.core.model.Flow;

class FlowPropertiesPage extends ModelPage<Flow> {

	private FormToolkit toolkit;

	FlowPropertiesPage(FlowEditor editor) {
		super(editor, "FlowPropertiesPage", Messages.FlowPropertiesPageLabel);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.Flow + ": "
				+ getModel().getName());
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);

		Section section = UI.section(body, toolkit,
				Messages.FlowPropertiesPageLabel);
		UI.gridData(section, true, true);
		Composite client = UI.sectionClient(section, toolkit);

		FlowPropertyFactorViewer factorViewer = new FlowPropertyFactorViewer(
				client, Database.getCache());
		getBinding().on(getModel(), "flowPropertyFactors", factorViewer);
		factorViewer.bindTo(section);

		body.setFocus();
		form.reflow(true);
	}

}
