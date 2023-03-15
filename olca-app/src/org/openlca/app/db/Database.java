package org.openlca.app.db;

import java.io.File;
import java.nio.file.Paths;
import java.util.Objects;

import org.openlca.app.M;
import org.openlca.app.navigation.CopyPaste;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.util.ErrorReporter;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.config.DatabaseConfig;
import org.openlca.core.database.config.DatabaseConfigList;
import org.openlca.core.database.config.DerbyConfig;
import org.openlca.core.database.config.MySqlConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Database management of the application.
 */
public class Database {

	private static IDatabase database;
	private static DatabaseConfig config;
	private static DatabaseListener listener;
	private static final DatabaseConfigList configurations = loadConfigs();

	private Database() {
	}

	public static boolean isNoneActive() {
		return get() == null;
	}

	public static IDatabase get() {
		return database;
	}

	public static WorkspaceIdUpdater getWorkspaceIdUpdater() {
		return listener.getWorkspaceIdUpdater();
	}

	public static IDatabase activate(DatabaseConfig config) {
		try {
			database = config.connect(Workspace.dbDir());
			listener = new DatabaseListener();
			database.addListener(listener);
			Cache.create(database);
			Database.config = config;
			Logger log = LoggerFactory.getLogger(Database.class);
			log.trace("activated database {} with version{}",
					database.getName(), database.getVersion());
			Repository.open(Repository.gitDir(database.getName()));
			return database;
		} catch (Exception e) {
			database = null;
			Cache.close();
			Database.config = null;
			ErrorReporter.on("failed to activate database: " + config, e);
			return null;
		}
	}

	public static boolean isActive(DatabaseConfig config) {
		if (config == null)
			return false;
		return Objects.equals(config, Database.config);
	}

	/**
	 * Closes the active database.
	 */
	public static void close() throws Exception {
		if (database == null)
			return;
		Cache.close();
		CopyPaste.clearCache();
		database.close();
		database = null;
		listener = null;
		config = null;
		Repository.close();
	}

	private static DatabaseConfigList loadConfigs() {
		var file = new File(Workspace.root(), "databases.json");
		return !file.exists()
				? new DatabaseConfigList()
				: DatabaseConfigList.read(file);
	}

	private static void saveConfig() {
		var file = new File(Workspace.root(), "databases.json");
		configurations.write(file);
	}

	public static DatabaseConfigList getConfigurations() {
		return configurations;
	}

	public static DatabaseConfig getActiveConfiguration() {
		for (var conf : configurations.getDerbyConfigs())
			if (isActive(conf))
				return conf;
		for (var conf : configurations.getMySqlConfigs())
			if (isActive(conf))
				return conf;
		return null;
	}

	public static void register(DerbyConfig config) {
		if (configurations.contains(config))
			return;
		configurations.getDerbyConfigs().add(config);
		saveConfig();
	}

	public static void remove(DerbyConfig config) {
		if (!configurations.contains(config))
			return;
		configurations.getDerbyConfigs().remove(config);
		saveConfig();
	}

	public static void register(MySqlConfig config) {
		if (configurations.contains(config))
			return;
		configurations.getMySqlConfigs().add(config);
		saveConfig();
	}

	public static void remove(MySqlConfig config) {
		if (!configurations.contains(config))
			return;
		configurations.getMySqlConfigs().remove(config);
		saveConfig();
	}

	/**
	 * Checks if the given string is a valid name for a new (local, file-based)
	 * database. Such a name is valid if a folder with that name can be created
	 * in the workspace and when no database with the same name already exists.
	 *
	 * @param name the name of the new database
	 * @return the validation error for display or {@code null} when the name is
	 * valid
	 */
	public static String validateNewName(String name) {
		if (name == null || name.isBlank() || name.length() < 1)
			return M.NewDatabase_NameToShort;
		if (!name.strip().equals(name)) {
			return "The given database name contains" +
					" leading or trailing whitespaces.";
		}
		try {
			var vDir = new File(Workspace.dbDir(), name);
			Paths.get(vDir.getAbsolutePath());
		} catch (Exception e) {
			return M.NewDatabase_InvalidName;
		}
		return getConfigurations().nameExists(name)
				? M.NewDatabase_AlreadyExists
				: null;
	}
}
