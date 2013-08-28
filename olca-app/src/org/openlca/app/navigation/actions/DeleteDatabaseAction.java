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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
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

	private List<IDatabaseConfiguration> configs;

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
		configs = Collections.singletonList(e.getContent());
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
		this.configs = config;
		return true;
	}

	@Override
	public String getText() {
		return Messages.DeleteDatabase;
	}

	@Override
	public void run() {
		if (configs == null || configs.isEmpty())
			return;
		if (createMessageDialog().open() != MessageDialog.OK)
			return;
		for (IDatabaseConfiguration config : this.configs) {
			try {
				tryDelete(config);
			} catch (Exception e) {
				log.error("failed to delete database {}", config);
			}
		}
		Navigator.refresh();
	}

	private void tryDelete(IDatabaseConfiguration config) throws Exception {
		if (Database.isActive(config)) {
			Editors.closeAll();
			Database.close();
		}
		if (config instanceof DerbyConfiguration)
			deleteDerbyDatabase(config);
		else if (config instanceof MySQLConfiguration)
			Database.remove((MySQLConfiguration) config);
	}

	private void deleteDerbyDatabase(IDatabaseConfiguration config)
			throws Exception {
		String path = ((DerbyConfiguration) config).getFolder()
				+ File.separator + config.getName();
		File folder = new File(path);
		if (folder.isDirectory())
			FileUtils.deleteDirectory(folder);
		Database.remove((DerbyConfiguration) config);
	}

	private MessageDialog createMessageDialog() {
		String name = configs.size() == 1 ? configs.get(0).getName()
				: "the selected databases";
		return new MessageDialog(UI.shell(), Messages.Delete, null, NLS.bind(
				Messages.NavigationView_DeleteQuestion, name),
				MessageDialog.QUESTION, new String[] {
						Messages.NavigationView_YesButton,
						Messages.NavigationView_NoButton, },
				MessageDialog.CANCEL);
	}

}
