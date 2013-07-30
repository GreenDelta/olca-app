/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.io.ilcd.importer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardDialog;
import org.openlca.app.UI;
import org.openlca.app.resources.ImageType;
import org.openlca.core.application.actions.IImportAction;
import org.openlca.core.database.IDatabase;

/**
 * Action for importing ILCD formatted files
 * 
 * @author Sebastian Greve
 * 
 */
public class ImportILCDAction extends Action implements IImportAction {

	/**
	 * The database
	 */
	private IDatabase database;

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageType.XML_ICON.getDescriptor();
	}

	@Override
	public String getText() {
		return Messages.ImportActionText;
	}

	@Override
	public void run() {
		if (database != null) {
			final WizardDialog dialog = new WizardDialog(UI.shell(),
					new ILCDImportWizard(database));
			dialog.open();
		}
	}

	@Override
	public void setDatabase(final IDatabase database) {
		this.database = database;
	}

}
