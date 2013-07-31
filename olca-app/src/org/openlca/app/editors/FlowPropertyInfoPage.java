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
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.Messages;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UIFactory;
import org.openlca.core.editors.ModelEditor;
import org.openlca.core.editors.ModelEditorInfoPage;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.UnitGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Information page of flow properties.
 */
public class FlowPropertyInfoPage extends ModelEditorInfoPage {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private FlowProperty flowProperty;
	private Text typeText;
	private Text unitGroupText;

	public FlowPropertyInfoPage(ModelEditor editor) {
		super(editor, "FlowPropertyInfoPage",
				Messages.Common_GeneralInformation,
				Messages.Common_GeneralInformation);
		flowProperty = (FlowProperty) editor.getModelComponent();
	}

	@Override
	protected void createContents(final Composite body,
			final FormToolkit toolkit) {
		super.createContents(body, toolkit);
		// TODO: create a hyper link to the model
		unitGroupText = UIFactory.createTextWithLabel(getMainComposite(),
				toolkit, Messages.UnitGroup, false);
		unitGroupText.setEditable(false);
		typeText = UIFactory.createTextWithLabel(getMainComposite(), toolkit,
				Messages.FlowProps_FlowPropertyType, false);
		typeText.setEditable(false);
	}

	@Override
	protected String getFormTitle() {
		final String title = Messages.Common_FlowProperty + ": "
				+ flowProperty.getName();
		return title;
	}

	@Override
	protected void setData() {
		super.setData();
		try {
			UnitGroup group = flowProperty.getUnitGroup();
			if (group != null && group.getName() != null)
				unitGroupText.setText(group.getName());
			typeText.setText(Labels.flowPropertyType(flowProperty));
		} catch (Exception e) {
			log.error("Failed to set editor data", e);
		}
	}
}
