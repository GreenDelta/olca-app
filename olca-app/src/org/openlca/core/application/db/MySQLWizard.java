/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.application.db;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.core.application.App;
import org.openlca.core.database.IDatabaseServer;
import org.openlca.core.database.mysql.MySQLServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * New wizard for registering a new MySQL server.
 */
public class MySQLWizard extends Wizard implements INewWizard {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public void addPages() {
		addPage(new MySQLPropertiesPage());
	}

	@Override
	public void init(final IWorkbench workbench,
			final IStructuredSelection selection) {
	}

	@Override
	public boolean performFinish() {
		MySQLPropertiesPage page = (MySQLPropertiesPage) getPage("MySQLPropertiesPage");
		try {
			Map<String, String> properties = new HashMap<>();
			properties.put(IDatabaseServer.HOST, page.getHost());
			properties.put(IDatabaseServer.PORT, page.getPort());
			properties.put(IDatabaseServer.USER, page.getUser());
			properties.put(IDatabaseServer.PASSWORD, page.getPassword());
			properties.put(IDatabaseServer.EMBEDDED, page.isEmbedded() + "");
			MySQLServer server = new MySQLServer();
			server.setProperties(properties);
			App.addDatabaseServer(server);
			return true;
		} catch (Exception e) {
			log.error("Could not create data provider", e);
			return false;
		}
	}
}
