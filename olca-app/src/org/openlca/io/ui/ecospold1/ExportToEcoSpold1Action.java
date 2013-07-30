/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.io.ui.ecospold1;

import java.util.Collections;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardDialog;
import org.openlca.core.application.actions.IExportAction;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.UI;

/**
 * Action for exporting a process or LCIA method in the EcoSpold01 format
 * 
 * @author Sebastian Greve
 * 
 */
public class ExportToEcoSpold1Action extends Action implements IExportAction {

	private BaseDescriptor component;

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageType.XML_ICON.getDescriptor();
	}

	@Override
	public String getText() {
		return Messages.ExportActionText;
	}

	@Override
	public void run() {
		if (component != null) {
			EcoSpold01ExportWizard wizard = new EcoSpold01ExportWizard(
					component.getModelType());
			wizard.setComponents(Collections.singletonList(component));
			WizardDialog dialog = new WizardDialog(UI.shell(), wizard);
			dialog.open();
		}
	}

	@Override
	public void setDescriptor(BaseDescriptor component) {
		this.component = component;
	}

}
