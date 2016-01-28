package org.openlca.app.db;

import java.util.concurrent.atomic.AtomicBoolean;

import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.Question;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.upgrades.Upgrades;
import org.openlca.core.database.upgrades.VersionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseUpdate implements Runnable {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final IDatabase database;
	private VersionState state;

	public DatabaseUpdate(IDatabase database) {
		this.database = database;
	}

	@Override
	public void run() {
		App.run("Checking database version",
				() -> state = Upgrades.checkVersion(database),
				this::handleVersionState);
	}

	private void handleVersionState() {
		if (state == null || state == VersionState.ERROR) {
			error(M.DatabaseVersionCheckFailed);
			return;
		}
		switch (state) {
		case NEWER:
			error(M.DatabaseNeedsUpdate);
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
		org.openlca.app.util.Error.showBox(M.CouldNotOpenDatabase,
				message);
		closeDatabase();
	}

	private void askRunUpdates() {
		boolean doIt = Question.ask(M.UpdateDatabase,
				M.UpdateDatabaseQuestion);
		if (!doIt) {
			closeDatabase();
			return;
		}
		AtomicBoolean failed = new AtomicBoolean(false);
		App.run(M.UpdateDatabase, () -> runUpdate(failed),
				() -> handleError(failed));
	}

	private void runUpdate(AtomicBoolean failed) {
		try {
			Upgrades.runUpgrades(database);
			database.getEntityFactory().getCache().evictAll();
		} catch (Exception e) {
			failed.set(true);
			log.error("Failed to update database", e);
		}
	}

	private void handleError(AtomicBoolean failed) {
		if (failed.get())
			closeDatabase();
		else 
			Navigator.refresh();
	}

	private void closeDatabase() {
		try {
			database.close();
		} catch (Exception e) {
			log.error("failed to close the database");
		} finally {
			Navigator.refresh();
		}
	}
}
