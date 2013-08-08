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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.db.DerbyConfiguration;
import org.openlca.app.db.IDatabaseConfiguration;
import org.openlca.app.db.MySQLConfiguration;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Editors;
import org.openlca.app.util.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deletes a database. Works only for local Derby databases; remote databases
 * cannot be deleted.
 */
public class DeleteDatabaseAction extends Action implements INavigationAction {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private List<IDatabaseConfiguration> config;

	public DeleteDatabaseAction() {
		setImageDescriptor(ImageType.DELETE_ICON.getDescriptor());
		setDisabledImageDescriptor(ImageType.DELETE_ICON_DISABLED
				.getDescriptor());
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof DatabaseElement))
			return false;
		DatabaseElement e = (DatabaseElement) element;
		config = Collections.singletonList(e.getContent());
		return true;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		List<IDatabaseConfiguration> config = new ArrayList<>();
		for (INavigationElement<?> element : elements) {
			if (!(element instanceof DatabaseElement))
				return false;
			DatabaseElement e = (DatabaseElement) element;
			config.add(e.getContent());
		}
		this.config = config;
		return true;
	}

	@Override
	public String getText() {
		return Messages.DeleteDatabase;
	}

	@Override
	public void run() {
		if (createMessageDialog().open() != 0)
			return;

		for (IDatabaseConfiguration config : this.config) {
			if (Database.isActive(config))
				try {
					Editors.closeAll();
					Database.close();
				} catch (Exception e) {
					log.error("Failed to close database", e);
					continue;
				}
			// TODO: really delete database
			if (config instanceof DerbyConfiguration)
				Database.remove((DerbyConfiguration) config);
			else if (config instanceof MySQLConfiguration)
				Database.remove((MySQLConfiguration) config);
		}
		Navigator.refresh();
	}

	private MessageDialog createMessageDialog() { 
		String name = config.size() == 1 ? config.get(0).getName() : "the selected databases";
		return new MessageDialog(UI.shell(), Messages.Delete, null, NLS.bind(
				Messages.NavigationView_DeleteQuestion, name),
				MessageDialog.QUESTION, new String[] {
						Messages.NavigationView_YesButton,
						Messages.NavigationView_NoButton, },
				MessageDialog.CANCEL);
	}
}
