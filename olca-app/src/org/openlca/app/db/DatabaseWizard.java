package org.openlca.app.db;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.openlca.app.M;
import org.openlca.app.cloud.ui.commits.HistoryView;
import org.openlca.app.cloud.ui.diff.CompareView;
import org.openlca.app.editors.Editors;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.UI;
import org.openlca.app.validation.ValidationView;
import org.openlca.core.database.config.DatabaseConfig;
import org.openlca.core.database.config.DerbyConfig;
import org.openlca.core.database.config.MySqlConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The wizard for database creation.
 */
public class DatabaseWizard extends Wizard {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private DatabaseWizardPage page;

	public DatabaseWizard() {
		setNeedsProgressMonitor(true);
		setWindowTitle(M.NewDatabase);
	}

	@Override
	public void addPages() {
		page = new DatabaseWizardPage();
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		try {
			if (!Editors.closeAll())
				return false;
			DatabaseConfig config = page.getPageData();
			Runner runner = (config instanceof DerbyConfig) ? new Runner(
					config, page.getSelectedContent()) : new Runner(config);
			getContainer().run(true, false, runner);
			Navigator.refresh();
			HistoryView.refresh();
			CompareView.clear();
			ValidationView.clear();
			return true;
		} catch (Exception e) {
			log.error("Database creation failed", e);
			return false;
		}
	}

	public static void open() {
		DatabaseWizard wizard = new DatabaseWizard();
		WizardDialog dialog = new WizardDialog(UI.shell(), wizard);
		dialog.open();
	}

	private class Runner implements IRunnableWithProgress {

		private DatabaseConfig config;
		private DbTemplate content;

		Runner(DatabaseConfig config) {
			this.config = config;
		}

		Runner(DatabaseConfig config, DbTemplate content) {
			this(config);
			this.content = content;
		}

		@Override
		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			monitor.beginTask(M.CreateDatabase,
					IProgressMonitor.UNKNOWN);
			try {
				Database.close();
				if (config instanceof MySqlConfig)
					Database.register((MySqlConfig) config);
				else if (config instanceof DerbyConfig) {
					Database.register((DerbyConfig) config);
					extractDerbyTemplate();
				}
				Database.activate(config);
			} catch (Exception e) {
				log.error("Create database failed", e);
			}
			monitor.done();
		}

		private void extractDerbyTemplate() {
			File dir = DatabaseDir.getRootFolder(config.name());
			if (dir.exists()) {
				log.error("could not create database {}; folder with name "
						+ "already exists", config);
				return;
			}
			try {
				content.extract(dir);
			} catch (Exception e) {
				log.error("failed to extract database template", e);
			}
		}

	}

}
