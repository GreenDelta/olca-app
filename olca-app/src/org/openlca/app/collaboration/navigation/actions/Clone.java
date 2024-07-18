package org.openlca.app.collaboration.navigation.actions;

import java.io.File;
import java.net.URISyntaxException;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.openlca.app.M;
import org.openlca.app.collaboration.util.Announcements;
import org.openlca.app.collaboration.util.CredentialStore;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.db.DatabaseDir;
import org.openlca.app.db.DbTemplate;
import org.openlca.app.db.Repository;
import org.openlca.app.util.Input;
import org.openlca.core.database.config.DerbyConfig;
import org.openlca.core.database.upgrades.Upgrades;
import org.openlca.git.actions.GitInit;
import org.openlca.util.Dirs;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Clone {

	private static final Logger log = LoggerFactory.getLogger(Clone.class);

	public static void of(String url, String user, String password) {
		var repoName = url.substring(url.lastIndexOf("/") + 1);
		var config = initDatabase(repoName);
		if (config == null)
			return;
		try {
			if (!initRepository(config.name(), url, user, password)) {
				onError(config);
				return;
			}
			PullAction.silent().run();
			Announcements.check();
		} catch (Exception e) {
			onError(config);
			Actions.handleException("Error importing repository", e);
		} finally {
			Cache.evictAll();
			Actions.refresh();
		}
	}

	private static DerbyConfig initDatabase(String name) {
		var dbDir = getDbDir(name);
		if (dbDir == null)
			return null;
		var config = new DerbyConfig();
		config.name(dbDir.getName());
		DbTemplate.EMPTY.extract(dbDir);
		var db = Database.activate(config);
		if (db == null)
			return null;
		Upgrades.on(db);
		Database.register(config);
		return config;
	}

	private static boolean initRepository(String dbName, String url, String user, String password)
			throws GitAPIException, URISyntaxException {
		var gitDir = Repository.gitDir(dbName);
		GitInit.in(gitDir).remoteUrl(url).run();
		var repo = Repository.initialize(gitDir, Database.get());
		if (repo == null)
			return false;
		repo.user(user);
		CredentialStore.put(url, user, password);
		return true;
	}

	private static void onError(DerbyConfig config) {
		try {
			Database.close();
			if (config != null) {
				Database.remove(config);
				Dirs.delete(Repository.gitDir(config.name()));
				Dirs.delete(DatabaseDir.getRootFolder(config.name()));
			}
		} catch (Exception e1) {
			log.error("Error deleting unused files", e1);
		}
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
