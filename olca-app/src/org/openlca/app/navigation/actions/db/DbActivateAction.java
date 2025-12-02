package org.openlca.app.navigation.actions.db;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
		long totalStart = System.nanoTime();
		log.info("PERF: DbActivateAction.run() started for database: {}", config.name());
		
		log.trace("Run database activation");
		if (Database.get() != null) {
			if (!Editors.closeAll())
				return;
		}

		// close a current database and open the new one
		var db = App.exec(M.OpenDatabaseDots, () -> {
			try {
				log.trace("Close other database if open");
				long closeStart = System.nanoTime();
				Database.close();
				long closeTime = System.nanoTime() - closeStart;
				log.debug("PERF: Database.close() completed in {} ms", 
						closeTime / 1_000_000.0);
				
				log.trace("Activate selected database");
				long connectStart = System.nanoTime();
				var connectedDb = config.connect(Workspace.dbDir());
				long connectTime = System.nanoTime() - connectStart;
				log.info("PERF: config.connect() in DbActivateAction completed in {} ms", 
						connectTime / 1_000_000.0);
				return connectedDb;
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
			long versionStart = System.nanoTime();
			version = VersionState.get(db);
			long versionTime = System.nanoTime() - versionStart;
			log.debug("PERF: VersionState.get() completed in {} ms", 
					versionTime / 1_000_000.0);
		} catch (Exception e) {
			ErrorReporter.on(
					"Failed to get version from database " + config.name(), e);
			return;
		}

		try {
			long callbackStart = System.nanoTime();
			new ActivationCallback(db, version).run();
			long callbackTime = System.nanoTime() - callbackStart;
			log.debug("PERF: ActivationCallback.run() completed in {} ms", 
					callbackTime / 1_000_000.0);
		} catch (Exception e) {
			ErrorReporter.on("Activation failed for database " + config.name(), e);
		}
		
		long totalTime = System.nanoTime() - totalStart;
		log.info("PERF: DbActivateAction.run() completed in {} ms (total)", 
				totalTime / 1_000_000.0);
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
					long exportStart = System.nanoTime();
					var success = new DbExportAction().run(config);
					long exportTime = System.nanoTime() - exportStart;
					log.info("PERF: Database export completed in {} ms", 
							exportTime / 1_000_000.0);
					if (!success) {
						error(M.DatabaseExportFailed);
						return;
					}
					long reconnectStart = System.nanoTime();
					db = config.connect(Workspace.dbDir());
					long reconnectTime = System.nanoTime() - reconnectStart;
					log.info("PERF: Database reconnection after export completed in {} ms", 
							reconnectTime / 1_000_000.0);
				} catch (Exception e) {
					error(M.DatabaseExportAndReconnectionFailed);
					return;
				}
			}

			// run database upgrades
			log.trace("Run database updates");
			// we change the database instance in another thread,
			// thus we pass an atomic reference around
			var nextDb = new AtomicReference<>(db);
			db = null;
			App.runWithProgress(M.UpdateDatabaseDots, () -> {
				try {
					var udb = nextDb.get();
					nextDb.set(null);
					
					long upgradesStart = System.nanoTime();
					Upgrades.on(udb);
					long upgradesTime = System.nanoTime() - upgradesStart;
					log.info("PERF: Upgrades.on() completed in {} ms", 
							upgradesTime / 1_000_000.0);
					
					udb.clearCache();
					
					long repoUpgradeStart = System.nanoTime();
					RepositoryUpgrade.on(udb);
					long repoUpgradeTime = System.nanoTime() - repoUpgradeStart;
					log.info("PERF: RepositoryUpgrade.on() completed in {} ms", 
							repoUpgradeTime / 1_000_000.0);
					
					udb.close();
					
					long reconnectStart = System.nanoTime();
					udb = config.connect(Workspace.dbDir());
					long reconnectTime = System.nanoTime() - reconnectStart;
					log.info("PERF: Database reconnection after upgrade completed in {} ms", 
							reconnectTime / 1_000_000.0);
					
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
			Navigator.revealFirstChild(navElem);
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
			var backupCheck = UI.checkbox(comp, M.CreateBackupOfTheDbFirst);
			backupCheck.setSelection(backupDatabase);
			Controls.onSelect(
					backupCheck, e -> backupDatabase = backupCheck.getSelection());
			return comp;
		}

	}

}
