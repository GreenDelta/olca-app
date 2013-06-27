/*******************************************************************************
 * Copyright (c) 2007 - 2013 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/

package org.openlca.app.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.core.application.Messages;
import org.openlca.core.application.db.DatabaseWizard;
import org.openlca.core.resources.ImageType;

/**
 * Opens the wizard for creating a new database.
 */
public class CreateDatabaseAction extends Action implements INavigationAction {

	@Override
	public boolean accept(INavigationElement<?> element) {
		return false;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

	@Override
	public void run() {
		DatabaseWizard.open();
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
