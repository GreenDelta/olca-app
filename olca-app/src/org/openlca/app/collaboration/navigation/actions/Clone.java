package org.openlca.app.collaboration.navigation.actions;

import java.io.File;

import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.collaboration.Repository;
import org.openlca.app.collaboration.util.Announcements;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.db.DatabaseDir;
import org.openlca.app.db.DbTemplate;
import org.openlca.app.util.Input;
import org.openlca.core.database.config.DerbyConfig;
import org.openlca.core.database.upgrades.Upgrades;
import org.openlca.util.Strings;

public class Clone {

	public static void of(String url, String user, String password) {
		var repoName = url.substring(url.lastIndexOf("/") + 1);
		var config = initDatabase(repoName);
		if (config == null)
			return;
		try {
			if (!initRepository(url, user, password))
				return;
			PullAction.silent().run();
			Announcements.check();
		} catch (Exception e) {
			Actions.handleException("Error importing repository", url, e);
		} finally {
			Cache.evictAll();
			Actions.refresh();
		}
	}

	private static DerbyConfig initDatabase(String name) {
		var dbDir = getDbDir(name);
		if (dbDir == null)
			return null;
		return App.exec(M.OpenDatabase, () -> {
			var config = new DerbyConfig();
			config.name(dbDir.getName());
			DbTemplate.EMPTY.extract(dbDir);
			var db = Database.activate(config);
			if (db == null)
				return null;
			Upgrades.on(db);
			Database.register(config);
			return config;
		});
	}

	private static boolean initRepository(String url, String user, String password) {
		var repo = Repository.initialize(Database.get(), url);
		if (repo == null)
			return false;
		repo.user(user);
		return true;
	}

	private static File getDbDir(String name) {
		var dir = DatabaseDir.getRootFolder(name);
		if (!dir.exists())
			return dir;
		name = Input.prompt(M.ImportGitRepository,
				M.PleaseEnterADatabaseName,
				name,
				v -> v,
				Database::validateNewName);
		if (Strings.nullOrEmpty(name))
			return null;
		return getDbDir(name);
	}

}
