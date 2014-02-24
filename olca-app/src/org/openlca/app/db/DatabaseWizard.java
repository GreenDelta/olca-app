package org.openlca.app.db;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.events.DatabaseCreatedEvent;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.Editors;
import org.openlca.app.util.UI;
import org.openlca.core.database.DatabaseContent;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.derby.DerbyDatabase;
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
		setWindowTitle(Messages.NewDatabase);
	}

	@Override
	public void addPages() {
		page = new DatabaseWizardPage();
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		try {
			Editors.closeAll();
			IDatabaseConfiguration config = page.getPageData();
			Runner runner = (config instanceof DerbyConfiguration) ? new Runner(
					config, page.getSelectedContent()) : new Runner(config);
			getContainer().run(true, false, runner);
			Navigator.refresh();
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

		private IDatabaseConfiguration config;
		private DatabaseContent content;

		Runner(IDatabaseConfiguration config) {
			this.config = config;
		}

		Runner(IDatabaseConfiguration config, DatabaseContent content) {
			this(config);
			this.content = content;
		}

		@Override
		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			monitor.beginTask(Messages.NewDatabase_Create,
					IProgressMonitor.UNKNOWN);
			try {
				if (config instanceof DerbyConfiguration)
					Database.register((DerbyConfiguration) config);
				else if (config instanceof MySQLConfiguration)
					Database.register((MySQLConfiguration) config);
				Database.close();
				IDatabase database = Database.activate(config);
				fillContent(database);
				App.getEventBus()
						.post(new DatabaseCreatedEvent(Database.get()));
			} catch (Exception e) {
				log.error("Create database failed", e);
			}
			Navigator.refresh();
			monitor.done();
		}

		private void fillContent(IDatabase database) {
			if (content == null || content == DatabaseContent.EMPTY)
				return;
			DerbyDatabase db = (DerbyDatabase) database;
			db.fill(content);
		}
	}

}
