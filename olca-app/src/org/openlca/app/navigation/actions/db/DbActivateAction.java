package org.openlca.app.navigation.actions.db;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.collaboration.navigation.actions.RepositoryUpgrade;
import org.openlca.app.collaboration.util.Announcements;
import org.openlca.app.collaboration.views.CompareView;
import org.openlca.app.collaboration.views.HistoryView;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.editors.Editors;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.config.DatabaseConfig;
import org.openlca.core.database.upgrades.Upgrades;
import org.openlca.core.database.upgrades.VersionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Activates a database with a version check and possible upgrade.
 */
public class DbActivateAction extends Action implements INavigationAction {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private DatabaseConfig config;

	public DbActivateAction() {
		setText(M.OpenDatabase);
		setImageDescriptor(Icon.CONNECT.descriptor());
	}

	public DbActivateAction(DatabaseConfig config) {
		this();
		this.config = config;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (!(first instanceof DatabaseElement e))
			return false;
		var config = e.getContent();
		if (Database.isActive(config))
			return false;
		this.config = config;
		return true;
	}

	@Override
	public void run() {
		log.trace("Run database activation");
		if (Database.get() != null) {
			if (!Editors.closeAll())
				return;
		}

		// close a current database and open the new one
		var db = App.exec("Open database...", () -> {
			try {
				log.trace("Close other database if open");
				Database.close();
				log.trace("Activate selected database");
				return config.connect(Workspace.dbDir());
			} catch (Exception e) {
				ErrorReporter.on("Failed to open database " + config.name(), e);
				return null;
			}
		});
		if (db == null) {
			Navigator.refresh();
			return;
		}

		// try to get the database version
		VersionState version;
		try {
			version = VersionState.get(db);
		} catch (Exception e) {
			ErrorReporter.on(
					"Failed to get version from database " + config.name(), e);
			return;
		}

		try {
			new ActivationCallback(db, version).run();
		} catch (Exception e) {
			ErrorReporter.on("Activation failed for database " + config.name(), e);
		}
	}

	private class ActivationCallback implements Runnable {

		private final VersionState version;
		private IDatabase db;

		ActivationCallback(IDatabase db, VersionState version) {
			this.db = db;
			this.version = version;
		}

		@Override
		public void run() {
			if (db == null || version == null)
				return;
			switch (version) {
				case HIGHER_VERSION:
					error(M.DatabaseNewerThanThisError);
					break;
				case NEEDS_UPGRADE:
					askRunUpgrades();
					break;
				case UP_TO_DATE:
					setActive(db);
					break;
				case ERROR:
					error(M.DatabaseVersionCheckFailed);
					break;
			}
		}

		private void askRunUpgrades() {

			// ask for an upgrade & backup
			var dialog = new UpgradeQuestionDialog();
			var doIt = dialog.open() == UpgradeQuestionDialog.OK;
			if (!doIt) {
				closeDatabase();
				return;
			}

			// run a backup
			if (dialog.backupDatabase) {
				try {
					db.close();
					db = null;
					var success = new DbExportAction().run(config);
					if (!success) {
						error("Database export failed");
						return;
					}
					db = config.connect(Workspace.dbDir());
				} catch (Exception e) {
					error("Database export and reconnection failed");
					return;
				}
			}

			// run database upgrades
			log.trace("Run database updates");
			// we change the database instance in another thread,
			// thus we pass an atomic reference around
			var nextDb = new AtomicReference<>(db);
			db = null;
			App.runWithProgress(M.UpdateDatabase, () -> {
				try {
					var udb = nextDb.get();
					nextDb.set(null);
					Upgrades.on(udb);
					udb.clearCache();
					RepositoryUpgrade.on(udb);
					udb.close();
					udb = config.connect(Workspace.dbDir());
					nextDb.set(udb);
				} catch (Exception e) {
					ErrorReporter.on("Database update failed", e);
					nextDb.set(null);
				}
			}, () -> {
				// set it as the active database
				var udb = nextDb.get();
				if (udb != null) {
					setActive(udb);
				}
			});
		}

		private void setActive(IDatabase db) {

			try {
				this.db = db;
				Database.setActive(config, db);
			} catch (Exception e) {
				ErrorReporter.on("Failed to activate database", e);
				closeDatabase();
				return;
			}

			// update the UI
			Navigator.refresh();
			var navElem = Navigator.findElement(config);
			if (navElem != null && !navElem.getChildren().isEmpty()) {
				var first = navElem.getChildren().get(0);
				var navigator = Navigator.getInstance();
				if (navigator != null) {
					var viewer = navigator.getCommonViewer();
					if (viewer != null) {
						viewer.reveal(first);
					}
				}
			}

			Repository.checkIfCollaborationServer();
			Announcements.check();
			HistoryView.refresh();
			CompareView.clear();
		}

		private void error(String message) {
			MsgBox.error(M.CouldNotOpenDatabase, message);
			closeDatabase();
		}

		private void closeDatabase() {
			try {
				if (db != null) {
					db.close();
					db = null;
				}
			} catch (Exception e) {
				log.error("failed to close the database");
			} finally {
				Navigator.refresh();
			}
		}
	}

	private static class UpgradeQuestionDialog extends Dialog {

		private boolean backupDatabase = true;

		public UpgradeQuestionDialog() {
			super(UI.shell());
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			getShell().setText(M.UpdateDatabase);
			var comp = (Composite) super.createDialogArea(parent);
			UI.label(comp, M.UpdateDatabaseQuestion);
			var backupCheck = UI.checkbox(
					comp, "Create a backup of the current database first");
			backupCheck.setSelection(backupDatabase);
			Controls.onSelect(
					backupCheck, e -> backupDatabase = backupCheck.getSelection());
			return comp;
		}

	}

}
