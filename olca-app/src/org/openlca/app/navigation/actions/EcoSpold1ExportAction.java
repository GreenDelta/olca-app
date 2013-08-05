/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.navigation.actions;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardDialog;
import org.openlca.app.Messages;
import org.openlca.app.io.ecospold1.EcoSpold01ExportWizard;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.UI;
import org.openlca.core.model.ModelType;

/**
 * Action for exporting a process or impact method in the EcoSpold01 format
 */
public class EcoSpold1ExportAction extends ExportAction {

	public EcoSpold1ExportAction() {
		super(ModelType.PROCESS, ModelType.IMPACT_METHOD);
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageType.XML_ICON.getDescriptor();
	}

	@Override
	public String getText() {
		return Messages.EcoSpoldExportActionText;
	}

	@Override
	public void run() {
		EcoSpold01ExportWizard wizard = new EcoSpold01ExportWizard(getType());
		wizard.setComponents(getComponents());
		WizardDialog dialog = new WizardDialog(UI.shell(), wizard);
		dialog.open();
	}

}
