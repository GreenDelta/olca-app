/*******************************************************************************
 * Copyright (c) 2007 - 2013 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/

package org.openlca.core.application.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openlca.core.application.Messages;
import org.openlca.core.application.wizards.DatabaseWizard;
import org.openlca.core.database.IDatabaseServer;
import org.openlca.core.resources.ImageType;

/**
 * Opens the wizard for creating a new database.
 */
public class CreateDatabaseAction extends Action {

	private final IDatabaseServer dataProvider;

	public CreateDatabaseAction(IDatabaseServer dataProvider) {
		this.dataProvider = dataProvider;
	}

	@Override
	public void run() {
		DatabaseWizard.open(dataProvider);
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageType.NEW_DB_ICON.getDescriptor();
	}

	@Override
	public String getText() {
		return Messages.CreateDatabaseAction;
	}

}
