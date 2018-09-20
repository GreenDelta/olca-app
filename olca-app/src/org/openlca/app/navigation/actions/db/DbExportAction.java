package org.openlca.app.navigation.actions.db;

import java.io.File;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.cloud.ui.commits.HistoryView;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Database;
import org.openlca.app.db.DatabaseDir;
import org.openlca.app.db.DerbyConfiguration;
import org.openlca.app.db.IDatabaseConfiguration;
import org.openlca.app.db.MySQLConfiguration;
import org.openlca.app.db.MySQLDatabaseExport;
import org.openlca.app.editors.Editors;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Info;
import org.openlca.cloud.api.RepositoryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.ZipUtil;

public class DbExportAction extends Action implements INavigationAction {

	private Logger log = LoggerFactory.getLogger(getClass());
	private DatabaseElement element;

	public DbExportAction() {
		setText(M.BackupDatabase);
		setImageDescriptor(Icon.DATABASE_EXPORT.descriptor());
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof DatabaseElement))
			return false;
		DatabaseElement e = (DatabaseElement) element;
		this.element = e;
		return true;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

	@Override
	public void run() {
		IDatabaseConfiguration config = null;
		if (element == null || element.getContent() == null) {
			config = Database.getActiveConfiguration();
		} else {
			config = element.getContent();
		}
		if (config == null)
			return;
		File file = FileChooser.forExport("*.zolca", config.getName()
				+ ".zolca");
		if (file == null)
			return;
		run(config, file, Database.isActive(config));
	}

	private void run(IDatabaseConfiguration config, final File zip,
			final boolean active) {
		if (zip.exists()) {
			log.trace("delete existing file {}", zip);
			boolean deleted = zip.delete();
			if (!deleted) {
				org.openlca.app.util.Error.showBox(M.CouldNotOverwriteFile
						+ ": " + zip.getName());
				return;
			}
		}
		if (active)
			if (!Editors.closeAll())
				return;
		log.trace("run database export to file {}", zip);
		App.runWithProgress(M.ExportDatabase, () -> realExport(config, zip, active));
		updateUI(zip, active);
	}

	private void realExport(IDatabaseConfiguration config, File zip,
			boolean active) {
		try {
			if (active)
				Database.close();
			if (config instanceof DerbyConfiguration) {
				File folder = DatabaseDir.getRootFolder(config.getName());
				ZipUtil.pack(folder, zip);
				ZipUtil.removeEntry(zip, DatabaseDir.FILE_STORAGE + "/" + RepositoryConfig.INDEX_DIR);
				ZipUtil.removeEntry(zip, DatabaseDir.FILE_STORAGE + "/" + RepositoryConfig.PROPERTIES_FILE);
			} else if (config instanceof MySQLConfiguration) {
				MySQLDatabaseExport export = new MySQLDatabaseExport(
						(MySQLConfiguration) config, zip);
				export.run();
			}
		} catch (Exception e) {
			log.error("Export failed " + zip, e);
		}
	}

	private void updateUI(final File zip, final boolean active) {
		if (active)
			Navigator.refresh();
		HistoryView.refresh();
		Info.popup(M.ExportDone, M.DatabaseWasExportedToFile
				+ ": " + zip.getName());
	}
}
