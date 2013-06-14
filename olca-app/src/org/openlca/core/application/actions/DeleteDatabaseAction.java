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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PartInitException;
import org.openlca.core.application.Messages;
import org.openlca.core.application.views.ModelEditorInput;
import org.openlca.core.application.views.search.SearchView;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.IDatabaseServer;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.Editors;
import org.openlca.ui.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deletes a database on a specified data provider.
 */
public class DeleteDatabaseAction extends NavigationAction {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private IDatabaseServer provider;
	private IDatabase database;

	public DeleteDatabaseAction(IDatabaseServer provider, String databaseName) {
		setImageDescriptor(ImageType.DELETE_ICON.getDescriptor());
		setDisabledImageDescriptor(ImageType.DELETE_ICON_DISABLED
				.getDescriptor());
		this.provider = provider;
		setDatabase(provider, databaseName);
	}

	private void setDatabase(IDatabaseServer dataProvider, String name) {
		try {
			for (IDatabase db : dataProvider.getConnectedDatabases()) {
				if (db.getName().equals(name)) {
					database = db;
					break;
				}
			}
		} catch (Exception e) {
			log.error("Could not set database", e);
		}
	}

	@Override
	protected String getTaskName() {
		return null;
	}

	@Override
	public String getText() {
		return Messages.DeleteDatabase;
	}

	@Override
	public void prepare() {
		try {
			closeEditors();
		} catch (Exception e) {
			log.error("Failed to close editors", e);
		}
	}

	private void closeEditors() throws PartInitException {
		for (IEditorReference ref : Editors.getReferences()) {
			IEditorInput input = ref.getEditorInput();
			if (input instanceof ModelEditorInput) {
				IDatabase database = ((ModelEditorInput) input).getDatabase();
				if (database.equals(this.database))
					Editors.close(ref);
			}
		}
	}

	@Override
	public void task() {
		try {
			if (createMessageDialog().open() == 0) {
				provider.delete(database);
			}
		} catch (Exception e) {
			log.error("Delete database failed", e);
		}
	}

	@Override
	public void after() {
		// TODO: we know that this will call Navigation.refresh
		// after the refresh the search view can be updated
		super.after();
		SearchView.refresh();
	}

	private MessageDialog createMessageDialog() {
		return new MessageDialog(UI.shell(), Messages.Common_Delete, null,
				NLS.bind(Messages.NavigationView_DeleteQuestion,
						database.getName()), MessageDialog.QUESTION,
				new String[] { Messages.NavigationView_YesButton,
						Messages.NavigationView_NoButton, },
				MessageDialog.CANCEL);
	}
}
