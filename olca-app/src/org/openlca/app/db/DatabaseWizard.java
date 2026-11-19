package org.openlca.app.db;

import java.lang.reflect.InvocationTargetException;

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
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.commons.Res;
import org.openlca.core.database.config.DatabaseConfig;
import org.openlca.core.database.config.DerbyConfig;
import org.openlca.core.database.config.MySqlConfig;
import org.openlca.core.database.upgrades.Upgrades;

/// The wizard for database creation.
public class DatabaseWizard extends Wizard {

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
			var content = config instanceof DerbyConfig
				? page.getSelectedContent()
				: null;
			var runner = new Runner(config, content);
			getContainer().run(true, false, runner);

			// expected errors, like missing write permissions, are shown to the
			// user; the wizard stays open
			var res = runner.result;
			if (res.isError()) {
				MsgBox.error(M.FailedToCreateDatabase, res.error());
				return false;
			}
			refreshViews();
			return true;
		} catch (Exception e) {
			var cause = e instanceof InvocationTargetException ite
				&& ite.getCause() != null
				? ite.getCause()
				: e;
			ErrorReporter.on("Failed to create database", cause);
			// the database could be activated before the error occurred
			refreshViews();
			return false;
		}
	}

	private void refreshViews() {
		Navigator.refresh();
		HistoryView.refresh();
		CompareView.clear();
	}

	/// Creates the database in a background thread. Expected errors are stored
	/// in the result; unexpected errors are thrown as invocation target
	/// exception, which is the contract of `IRunnableWithProgress`.
	private static class Runner implements IRunnableWithProgress {

		private final DatabaseConfig config;
		private final DbTemplate content;
		private volatile Res<Void> result = Res.ok();

		/// The template is only used for Derby databases and can be `null` for
		/// other database types.
		Runner(DatabaseConfig config, DbTemplate content) {
			this.config = config;
			this.content = content;
		}

		@Override
		public void run(IProgressMonitor monitor)
			throws InvocationTargetException {
			monitor.beginTask(M.CreateDatabase, IProgressMonitor.UNKNOWN);
			try {
				result = createIt();
			} catch (Exception e) {
				throw new InvocationTargetException(e);
			} finally {
				monitor.done();
			}
		}

		private Res<Void> createIt() {
			if (config instanceof MySqlConfig mySql) {
				Database.activate(mySql);
				Database.register(mySql);
				return Res.ok();
			}

			// check that we can extract a Derby database
			if (!(config instanceof DerbyConfig derby))
				return Res.error("Unknown database config: " + config);
			if (content == null)
				return Res.error("No database template selected");
			var dir = DatabaseDir.getRootFolder(derby.name());
			if (dir.exists())
				return Res.error(M.NewDatabase_AlreadyExists + ": " + dir);

			// extract the database template and run possible
			// upgrades if required
			var res = content.extract(dir);
			if (res.isError())
				return res;
			var db = Database.activate(derby);
			if (db != null) {
				Upgrades.on(db);
				Database.register(derby);
			}
			return Res.ok();
		}
	}
}
