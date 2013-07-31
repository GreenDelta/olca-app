package org.openlca.core.application.actions;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.openlca.app.Messages;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.UI;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.IDatabaseServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exports a database to a 'olca'-file.
 */
public class ExportDatabaseAction extends NavigationAction {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private final IDatabaseServer dataProvider;
	private final IDatabase database;
	private String filePath;

	public ExportDatabaseAction(IDatabaseServer dataProvider, IDatabase database) {
		this.dataProvider = dataProvider;
		this.database = database;
		setId("org.openlca.core.application.actions.ExportDatabaseAction");
		setImageDescriptor(ImageType.DB_IO.getDescriptor());
	}

	@Override
	public String getText() {
		return Messages.ExportToScript;
	}

	@Override
	protected String getTaskName() {
		return Messages.bind(Messages.ExportingToScript, database.getName());
	}

	@Override
	public void prepare() {
		final FileDialog fileDialog = new FileDialog(UI.shell(), SWT.SAVE);
		fileDialog.setOverwrite(true);
		fileDialog.setText(database.getName() + ".olca");
		fileDialog.setFilterExtensions(new String[] { "*.olca" });
		filePath = fileDialog.open();
	}

	@Override
	protected void task() {
		if (filePath == null)
			return;
		try {
			File file = new File(filePath);
			if (!file.exists()) {
				file.createNewFile();
			}
			dataProvider.exportDatabase(database, file);
		} catch (Exception e) {
			log.error("Export database failed", e);
		}
	}

}
