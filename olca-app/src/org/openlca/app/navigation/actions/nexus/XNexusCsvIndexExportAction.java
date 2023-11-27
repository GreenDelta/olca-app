package org.openlca.app.navigation.actions.nexus;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.forms.FormDialog;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.database.ProductSystemDao;
import org.openlca.core.model.ProcessType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XNexusCsvIndexExportAction extends Action implements INavigationAction {

	private final Logger log = LoggerFactory.getLogger(getClass());

	public XNexusCsvIndexExportAction() {
		setImageDescriptor(Icon.EXTENSION.descriptor());
		setText("Export Nexus CSV Index");
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (!(first instanceof DatabaseElement))
			return false;
		var e = (DatabaseElement) first;
		return Database.isActive(e.getContent());
	}

	@Override
	public void run() {
		var db = Database.get();
		if (db == null) {
			log.trace("no database activated");
			return;
		}
		var defaultName = db.getName() + "_nexus_index.csv";
		var file = FileChooser.forSavingFile(M.Export, defaultName);
		if (file == null)
			return;
		var dialog = new MetaDataDialog();
		if (dialog.open() == FormDialog.CANCEL)
			return;
		var metaData = dialog.getMetaData();
		App.run("Export Nexus CSV index", new Runner(file, metaData, db));
	}

	private class Runner implements Runnable {

		private final File file;
		private final MetaData metaData;
		private final IDatabase db;

		public Runner(File file, MetaData metaData, IDatabase db) {
			this.file = file;
			this.metaData = metaData;
			this.db = db;
		}

		@Override
		public void run() {
			log.trace("run nexus csv index export");
			try {
				var entries = new ArrayList<IndexEntry>();
				var unitProcessIds = new HashSet<Long>();
				var processDao = new ProcessDao(db);
				for (var d : processDao.getDescriptors()) {
					var process = processDao.getForId(d.id);
					entries.add(new ProcessIndexEntry(process, metaData));
					if (d.processType == ProcessType.UNIT_PROCESS) {
						unitProcessIds.add(d.id);
					}
				}
				if (metaData.exportSystems) {
					var systemDao = new ProductSystemDao(db);
					for (var d : systemDao.getDescriptors()) {
						var system = systemDao.getForId(d.id);
						entries.add(new ProductSystemIndexEntry(system, metaData, unitProcessIds));
					}
				}
				CsvWriter.write(file, entries);
			} catch (Exception e) {
				log.error("failed to write csv index entries", e);
			}
		}

	}

}
