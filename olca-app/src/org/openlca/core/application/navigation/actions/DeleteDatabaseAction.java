/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.application.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.openlca.core.application.Messages;
import org.openlca.core.application.db.Database;
import org.openlca.core.application.db.DerbyConfiguration;
import org.openlca.core.application.navigation.DatabaseElement;
import org.openlca.core.application.navigation.INavigationElement;
import org.openlca.core.application.views.navigator.Navigator;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.Editors;
import org.openlca.ui.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deletes a database. Works only for local Derby databases; remote databases
 * cannot be deleted.
 */
public class DeleteDatabaseAction extends Action implements INavigationAction {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private DerbyConfiguration config;

	public DeleteDatabaseAction() {
		setImageDescriptor(ImageType.DELETE_ICON.getDescriptor());
		setDisabledImageDescriptor(ImageType.DELETE_ICON_DISABLED
				.getDescriptor());
	}

	@Override
	public boolean accept(INavigationElement element) {
		if (!(element instanceof DatabaseElement))
			return false;
		DatabaseElement e = (DatabaseElement) element;
		if (!(e.getData() instanceof DerbyConfiguration))
			return false;
		this.config = (DerbyConfiguration) e.getData();
		return true;
	}

	@Override
	public boolean accept(List<INavigationElement> elements) {
		return false;
	}

	@Override
	public String getText() {
		return Messages.DeleteDatabase;
	}

	@Override
	public void run() {
		if (createMessageDialog().open() != 0)
			return;
		if (Database.isActive(config))
			try {
				Editors.closeAll();
				Database.close();
			} catch (Exception e) {
				log.error("Failed to close database", e);
				return;
			}
		// TODO: really delete database
		Database.remove(config);
		Navigator.refresh();
	}

	private MessageDialog createMessageDialog() {
		return new MessageDialog(UI.shell(), Messages.Common_Delete, null,
				NLS.bind(Messages.NavigationView_DeleteQuestion,
						config.getName()), MessageDialog.QUESTION,
				new String[] { Messages.NavigationView_YesButton,
						Messages.NavigationView_NoButton, },
				MessageDialog.CANCEL);
	}
}
