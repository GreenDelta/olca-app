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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.openlca.core.application.Messages;
import org.openlca.core.application.views.ModelEditorInput;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.IDatabaseServer;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.Editors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action for connecting / disconnecting to a database server.
 */
public class ServerConnectionAction extends Action {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private IDatabaseServer dataProvider;
	private ServerConnector connector = new ServerConnector();

	public ServerConnectionAction(IDatabaseServer dataProvider) {
		this.dataProvider = dataProvider;
		String text = null;
		ImageDescriptor image = null;
		if (dataProvider.isRunning()) {
			text = Messages.ConnectDataProviderAction_Disconnect;
			image = ImageType.DISCONNECT_ICON.getDescriptor();
		} else {
			text = Messages.ConnectDataProviderAction_Connect;
			image = ImageType.CONNECT_ICON.getDescriptor();
		}
		setText(text);
		setImageDescriptor(image);
	}

	@Override
	public void run() {
		try {
			if (dataProvider.isRunning()) {
				closeEditors();
				connector.disconnect(dataProvider);
			} else
				connector.connect(dataProvider);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void closeEditors() throws Exception {
		for (IEditorReference reference : Editors.getReferences()) {
			IEditorInput input = reference.getEditorInput();
			if (input instanceof ModelEditorInput) {
				IDatabase db = ((ModelEditorInput) input).getDatabase();
				if (fromDataProvider(db))
					Editors.close(reference);
			}
		}
	}

	private boolean fromDataProvider(IDatabase database) throws Exception {
		for (IDatabase db : dataProvider.getConnectedDatabases()) {
			if (db.equals(database))
				return true;
		}
		return false;
	}

}
