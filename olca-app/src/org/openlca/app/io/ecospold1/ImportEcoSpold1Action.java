/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.io.ecospold1;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardDialog;
import org.openlca.app.Messages;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.UI;
import org.openlca.core.database.IDatabase;

/**
 * Action for importing EcoSpold01 formatted files
 */
public class ImportEcoSpold1Action extends Action {

	private IDatabase database;

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageType.XML_ICON.getDescriptor();
	}

	@Override
	public String getText() {
		return Messages.EcoSpoldImportActionText;
	}

	@Override
	public void run() {
		if (database != null) {
			final WizardDialog dialog = new WizardDialog(UI.shell(),
					new EcoSpold01ImportWizard());
			dialog.open();
		}
	}

	public void setDatabase(final IDatabase database) {
		this.database = database;
	}

}
