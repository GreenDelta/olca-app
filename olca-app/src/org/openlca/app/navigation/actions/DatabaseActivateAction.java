package org.openlca.app.navigation.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.App;
import org.openlca.app.db.Database;
import org.openlca.app.db.IDatabaseConfiguration;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Editors;
import org.openlca.app.util.Question;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.upgrades.Upgrades;
import org.openlca.core.database.upgrades.VersionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Activates a database with a version check and possible upgrade.
 */
public class DatabaseActivateAction extends Action implements INavigationAction {

	private Logger log = LoggerFactory.getLogger(getClass());
	private IDatabaseConfiguration config;

	public DatabaseActivateAction() {
		setText("Activate");
		setImageDescriptor(ImageType.CONNECT_ICON.getDescriptor());
	}

	public DatabaseActivateAction(IDatabaseConfiguration config) {
		this();
		this.config = config;
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof DatabaseElement))
			return false;
		DatabaseElement e = (DatabaseElement) element;
		IDatabaseConfiguration config = e.getContent();
		if (Database.isActive(config))
			return false;
		this.config = config;
		return true;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

	@Override
	public void run() {
		if (Database.get() != null)
			Editors.closeAll();
		Activation activation = new Activation();
		// App.run does not work as we have to show a modal dialog in the
		// callback
		try {
			PlatformUI.getWorkbench().getProgressService()
					.busyCursorWhile(activation);
			ActivationCallback callback = new ActivationCallback(activation);
			callback.run();
		} catch (Exception e) {
			log.error("Database activation failed");
		}
	}

	private class Activation implements IRunnableWithProgress {

		private VersionState versionState;

		@Override
		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			try {
				monitor.beginTask("Activate database", IProgressMonitor.UNKNOWN);
				Database.close();
				IDatabase database = Database.activate(config);
				versionState = Upgrades.checkVersion(database);
				monitor.done();
			} catch (Exception e) {
				log.error("Failed to activate database", e);
			}
		}
	}

	private class ActivationCallback implements Runnable {

		private Activation activation;

		ActivationCallback(Activation activation) {
			this.activation = activation;
		}

		@Override
		public void run() {
			if (activation == null)
				return;
			VersionState state = activation.versionState;
			if (state == null || state == VersionState.ERROR) {
				error("Could not get the version from the database. Is this an "
						+ "openLCA database?");
				return;
			}
			handleVersionState(state);
		}

		private void handleVersionState(VersionState state) {
			switch (state) {
			case NEWER:
				error("The given database is newer than this openLCA version.");
				break;
			case OLDER:
				askRunUpdates();
				break;
			case CURRENT:
				Navigator.refresh();
				break;
			default:
				break;
			}
		}

		private void error(String message) {
			org.openlca.app.util.Error.showBox("Could not open database",
					message);
			closeDatabase();
		}

		private void askRunUpdates() {
			IDatabase db = Database.get();
			boolean doIt = Question.ask("Run update?",
					"The database " + db.getName()
							+ " needs an update. Do you want to run it?");
			if (!doIt) {
				closeDatabase();
				return;
			}
			AtomicBoolean failed = new AtomicBoolean(false);
			App.run("Update database",
					() -> runUpdate(db, failed),
					() -> {
						if (failed.get())
							closeDatabase();
						else
							Navigator.refresh();
					});
		}

		private void runUpdate(IDatabase db, AtomicBoolean failed) {
			try {
				Upgrades.runUpgrades(db);
				db.getEntityFactory().getCache().evictAll();
			} catch (Exception e) {
				failed.set(true);
				log.error("Failed to update database", e);
			}
		}

		private void closeDatabase() {
			try {
				Database.close();
			} catch (Exception e) {
				log.error("failed to close the database");
			} finally {
				Navigator.refresh();
			}
		}
	}
}
