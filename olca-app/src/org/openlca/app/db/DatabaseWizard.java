package org.openlca.app.db;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.openlca.app.M;
import org.openlca.app.collaboration.views.CompareView;
import org.openlca.app.collaboration.views.HistoryView;
import org.openlca.app.editors.Editors;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.UI;
import org.openlca.core.database.config.DatabaseConfig;
import org.openlca.core.database.config.DerbyConfig;
import org.openlca.core.database.config.MySqlConfig;
import org.openlca.core.database.upgrades.Upgrades;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The wizard for database creation.
 */
public class DatabaseWizard extends Wizard {

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private final String folder;
	private DatabaseWizardPage page;

	private DatabaseWizard(String folder) {
		this.folder = folder;
		setNeedsProgressMonitor(true);
		setWindowTitle(M.NewDatabase);
	}

	public static void open() {
		open("");
	}

	public static void open(String folder) {
		var wizard = new DatabaseWizard(folder);
		var dialog = new WizardDialog(UI.shell(), wizard);
		dialog.open();
	}

	@Override
	public void addPages() {
		page = new DatabaseWizardPage(folder);
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		try {
			if (!Editors.closeAll())
				return false;
			var config = page.getPageData();
			var runner = (config instanceof DerbyConfig)
					? new Runner(config, page.getSelectedContent())
					: new Runner(config);
			getContainer().run(true, false, runner);
			Navigator.refresh();
			HistoryView.refresh();
			CompareView.clear();
			return true;
		} catch (Exception e) {
			log.error("Database creation failed", e);
			return false;
		}
	}

	private static class Runner implements IRunnableWithProgress {

		private final DatabaseConfig config;
		private DbTemplate content;

		Runner(DatabaseConfig config) {
			this.config = config;
		}

		Runner(DatabaseConfig config, DbTemplate content) {
			this(config);
			this.content = content;
		}

		@Override
		public void run(IProgressMonitor monitor) {
			monitor.beginTask(M.CreateDatabase, IProgressMonitor.UNKNOWN);
			try {
				createIt();
			} catch (Exception e) {
				ErrorReporter.on("failed to create database", e);
			}
			monitor.done();
		}

		private void createIt() {
			if (config instanceof MySqlConfig) {
				Database.activate(config);
				Database.register((MySqlConfig) config);
				return;
			}

			// check that we can extract a Derby database
			if (!(config instanceof DerbyConfig)) {
				ErrorReporter.on("Unknown database config: " + config);
				return;
			}
			var dir = DatabaseDir.getRootFolder(config.name());
			if (dir.exists()) {
				ErrorReporter.on("Failed to create database: folder "
						+ dir + " already exists");
				return;
			}

			// extract the database template and run possible
			// upgrades if required
			content.extract(dir);
			var db = Database.activate(config);
			if (db != null) {
				Upgrades.on(db);
				Database.register((DerbyConfig) config);
			}
		}
	}
}
