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
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.*;
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
		IDatabaseConfiguration config = e.getContent();
		if (!config.isLocal())
			return false;
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
		IDatabaseConfiguration c = element.getContent();
		if (!(c instanceof DerbyConfiguration))
			return;
		DerbyConfiguration config = (DerbyConfiguration) c;
		File file = FileChooser.forExport("*.zolca", config.getName()
				+ ".zolca");
		if (file == null)
			return;
		if (Database.isActive(config))
			run(config, file, true);
		else
			run(config, file, false);
	}

	private void run(final DerbyConfiguration config, final File zip,
			final boolean active) {
		if (zip.exists()) {
			log.trace("delete existing file {}", zip);
			boolean deleted = zip.delete();
			if(!deleted) {
				org.openlca.app.util.Error.showBox("Could not overwrite " +
				zip.getName());
				return;
			}
		}
		if (active)
			Editors.closeAll();
		log.trace("run database export to file {}", zip);
		App.run("Export database", new Runnable() {
			public void run() {
				realExport(config, zip, active);
			}
		}, new Runnable() {
			public void run() {
				updateUI(zip, active);
			}
		});
	}

	private void realExport(final DerbyConfiguration config, final File zip,
			final boolean active) {
		try {
			if (active)
				Database.close();
			File folder = DatabaseFolder.getRootFolder(config.getName());
			ZipUtil.pack(folder, zip);
		} catch (Exception e) {
			log.error("Export failed " + zip, e);
		}
	}

	private void updateUI(final File zip, final boolean active) {
		if (active)
			Navigator.refresh();
		InformationPopup.show("Export done", "Database was exported to file "
				+ zip.getName());
	}
}
