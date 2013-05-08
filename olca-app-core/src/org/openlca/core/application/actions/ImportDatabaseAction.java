package org.openlca.core.application.actions;

import java.io.File;
import java.util.List;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.openlca.core.application.Messages;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.IDatabaseServer;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action for importing openLCA databases
 */
public class ImportDatabaseAction extends NavigationAction {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private IDatabaseServer dataProvider;
	private String file;
	private String newDatabase;

	public ImportDatabaseAction(IDatabaseServer dataProvider) {
		this.dataProvider = dataProvider;
		setId("org.openlca.core.application.actions.ImportDatabaseAction");
		setImageDescriptor(ImageType.DB_IO.getDescriptor());
	}

	@Override
	public String getText() {
		return Messages.ImportScript;
	}

	@Override
	protected String getTaskName() {
		return Messages.ImportingScript;
	}

	@Override
	public void prepare() {
		try {
			final FileDialog fileDialog = new FileDialog(UI.shell(), SWT.OPEN);
			fileDialog.setFilterExtensions(new String[] { "*.olca" });
			file = fileDialog.open();
			if (file != null) {
				final List<IDatabase> dbs = dataProvider
						.getConnectedDatabases();
				final String[] databaseNames = new String[dbs.size()];
				for (int i = 0; i < dbs.size(); i++) {
					databaseNames[i] = dbs.get(i).getName();
				}
				final File f = new File(file);
				final InputDialog inputDialog = new InputDialog(UI.shell(),
						"Create Database", "Database Name", f.getName()
								.substring(0, f.getName().indexOf('.')),
						new DatabaseInputValidator(databaseNames));
				inputDialog.open();
				if (inputDialog.getReturnCode() == Window.OK) {
					newDatabase = inputDialog.getValue();
				}
			}
		} catch (Exception e) {
			log.error("Prepare failed", e);
		}
	}

	@Override
	protected void task() {
		if (file != null && newDatabase != null) {
			try {
				dataProvider.importDatabase(newDatabase, new File(file));
			} catch (Exception e) {
				log.error("Import database failed", e);
			}
		}
	}
}
