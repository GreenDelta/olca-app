package org.openlca.app.navigation.actions.db;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.cloud.ui.commits.HistoryView;
import org.openlca.app.components.UpdateManager;
import org.openlca.app.db.Database;
import org.openlca.app.db.IDatabaseConfiguration;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Editors;
import org.openlca.app.util.Question;
import org.openlca.core.database.IDatabase;
import org.openlca.updates.VersionState;
import org.openlca.updates.legacy.Upgrades;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Activates a database with a version check and possible upgrade.
 */
public class DbActivateAction extends Action implements INavigationAction {

	private Logger log = LoggerFactory.getLogger(getClass());
	private IDatabaseConfiguration config;

	public DbActivateAction() {
		setText(M.OpenDatabase);
		setImageDescriptor(Icon.CONNECT.descriptor());
	}

	public DbActivateAction(IDatabaseConfiguration config) {
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
		log.trace("Run database activation");
		if (Database.get() != null) {
			if (!Editors.closeAll())
				return;
		}

		Activation activation = new Activation();
		// App.run does not work as we have to show a modal dialog in the
		// callback
		try {
			PlatformUI.getWorkbench()
					.getProgressService()
					.busyCursorWhile(activation);
			new ActivationCallback(activation).run();
		} catch (Exception e) {
			log.error("Database activation failed", e);
		}
	}

	private class Activation implements IRunnableWithProgress {

		private VersionState versionState;

		@Override
		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			try {
				monitor.beginTask(M.OpenDatabase, IProgressMonitor.UNKNOWN);
				log.trace("Close other database if open");
				Database.close();
				log.trace("Activate selected database");
				IDatabase db = Database.activate(config);
				log.trace("Get version state");
				versionState = VersionState.checkVersion(db);
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
				error(M.DatabaseVersionCheckFailed);
				return;
			}
			handleVersionState(state);
		}

		private void handleVersionState(VersionState state) {
			log.trace("Check version state");
			switch (state) {
			case HIGHER_VERSION:
				error(M.DatabaseNeedsUpdate);
				break;
			case NEEDS_UPGRADE:
				askRunUpgrades();
				break;
			case NEEDS_UPDATE:
				if (UpdateManager.openNewAndRequired()) {
					refresh();
				} else {
					closeDatabase();
				}
				break;
			case UP_TO_DATE:
				refresh();
				break;
			default:
				break;
			}
		}

		private void refresh() {
			log.trace("Refresh navigation");
			Navigator.refresh();
			if (Database.get() == null)
				return;
			INavigationElement<?> dbElem = Navigator.findElement(config);
			INavigationElement<?> firstModelType = dbElem.getChildren().get(0);
			Navigator.getInstance().getCommonViewer().reveal(firstModelType);
			log.trace("Refresh history view (if open)");
			HistoryView.refresh();
		}

		private void error(String message) {
			org.openlca.app.util.Error.showBox(M.CouldNotOpenDatabase, message);
			closeDatabase();
		}

		private void askRunUpgrades() {
			IDatabase db = Database.get();
			boolean doIt = Question.ask(M.UpdateDatabase, M.UpdateDatabaseQuestion);
			if (!doIt) {
				closeDatabase();
				return;
			}
			log.trace("Run database updates");
			AtomicBoolean failed = new AtomicBoolean(false);
			App.run(M.UpdateDatabase,
					() -> runUpgrades(db, failed),
					() -> {
						closeDatabase();
						DbActivateAction.this.run();
					});
		}

		private void runUpgrades(IDatabase db, AtomicBoolean failed) {
			try {
				Database.getIndexUpdater().disable();
				Upgrades.runUpgrades(db);
				db.getEntityFactory().getCache().evictAll();
				Database.getIndexUpdater().enable();
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
