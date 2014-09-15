package org.openlca.app.navigation.actions;

import java.io.File;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Database;
import org.openlca.app.db.DatabaseFolder;
import org.openlca.app.db.DerbyConfiguration;
import org.openlca.app.db.IDatabaseConfiguration;
import org.openlca.app.db.MySQLConfiguration;
import org.openlca.app.db.MySQLDatabaseExport;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Editors;
import org.openlca.app.util.InformationPopup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.ZipUtil;

public class DatabaseExportAction extends Action implements INavigationAction {

	private Logger log = LoggerFactory.getLogger(getClass());
	private DatabaseElement element;

	public DatabaseExportAction() {
		setText(Messages.ExportDatabase);
		setImageDescriptor(ImageType.DB_IO.getDescriptor());
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
		if (element == null || element.getContent() == null)
			return;
		IDatabaseConfiguration config = element.getContent();
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
				org.openlca.app.util.Error.showBox(Messages.CouldNotOverwriteFile
						+ ": " + zip.getName());
				return;
			}
		}
		if (active)
			Editors.closeAll();
		log.trace("run database export to file {}", zip);
		App.run(Messages.ExportDatabase,
				() -> realExport(config, zip, active),
				() -> updateUI(zip, active));
	}

	private void realExport(IDatabaseConfiguration config, File zip,
			boolean active) {
		try {
			if (active)
				Database.close();
			if (config instanceof DerbyConfiguration) {
				File folder = DatabaseFolder.getRootFolder(config.getName());
				ZipUtil.pack(folder, zip);
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
		InformationPopup.show(Messages.ExportDone, Messages.DatabaseWasExportedToFile
				+ ": " + zip.getName());
	}
}
