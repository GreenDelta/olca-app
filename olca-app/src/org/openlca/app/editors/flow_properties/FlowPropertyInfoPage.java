/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/

package org.openlca.app.editors.flow_properties;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.Messages;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.UI;
import org.openlca.core.model.FlowProperty;

/**
 * Information page of flow properties.
 */
class FlowPropertyInfoPage extends ModelPage<FlowProperty> {

	private FormToolkit toolkit;

	FlowPropertyInfoPage(FlowPropertyEditor editor) {
		super(editor, "FlowPropertyInfoPage", Messages.GeneralInformation);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.FlowProperty
				+ ": " + getModel().getName());
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		InfoSection infoSection = new InfoSection(getEditor());
		infoSection.render(body, toolkit);
		createAdditionalInfo(infoSection);
		body.setFocus();
		form.reflow(true);
	}

	private void createAdditionalInfo(InfoSection infoSection) {
		createLink(Messages.UnitGroup, "unitGroup", infoSection.getContainer());
		createReadOnly(Messages.FlowProps_FlowPropertyType, "flowPropertyType",
				infoSection.getContainer());
	}
}
