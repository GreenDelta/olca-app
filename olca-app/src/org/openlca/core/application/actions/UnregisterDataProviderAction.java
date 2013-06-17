/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.application.actions;

import org.openlca.core.application.Messages;
import org.openlca.core.database.IDatabaseServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This action unregisters a data provider
 */
public class UnregisterDataProviderAction extends NavigationAction {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private IDatabaseServer dataProvider;

	public UnregisterDataProviderAction(IDatabaseServer dataProvider) {
		this.dataProvider = dataProvider;
	}

	@Override
	protected String getTaskName() {
		return null;
	}

	@Override
	public String getText() {
		return Messages.UnregisterDataProviderAction_Text;
	}

	@Override
	public void task() {
		// TODO close model editors, see connect provider action
		// try {
		// if (dataProvider.isRunning()) {
		// for (final IEditorReference reference : PlatformUI
		// .getWorkbench().getWorkbenchWindows()[0]
		// .getActivePage().getEditorReferences()) {
		// if (reference.getEditorInput() instanceof ModelEditorInput) {
		// final IDatabase database = ((ModelEditorInput) reference
		// .getEditorInput()).getDatabase();
		// boolean containsDatabase = false;
		// for (IDatabase provDb : dataProvider
		// .getConnectedDatabases()) {
		// if (provDb.equals(database)) {
		// containsDatabase = true;
		// break;
		// }
		// }
		// if (containsDatabase) {
		// PlatformUI.getWorkbench().getWorkbenchWindows()[0]
		// .getActivePage().closeEditor(
		// reference.getEditor(true), true);
		// }
		// }
		// }
		// dataProvider.shutdown();
		// }
		// } catch (final Exception e) {
		// log.error("Task failed", e);
		// }
		//
		// App.removeDatabaseServer(dataProvider);
	}
}
