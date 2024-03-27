package org.openlca.app.db;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Objects;

import org.openlca.app.M;
import org.openlca.app.navigation.CopyPaste;
import org.openlca.app.rcp.RcpWindowAdvisor;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.util.ErrorReporter;
import org.openlca.core.database.Derby;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.config.DatabaseConfig;
import org.openlca.core.database.config.DatabaseConfigList;
import org.openlca.core.database.config.DerbyConfig;
import org.openlca.core.database.config.MySqlConfig;
import org.slf4j.LoggerFactory;

/**
 * Database management of the application.
 */
public class Database {

	private static IDatabase database;
	private static DatabaseConfig config;
	private static final DatabaseConfigList configurations = readConfigs();

	private Database() {
	}

	public static boolean isNoneActive() {
		return get() == null;
	}

	public static IDatabase get() {
		return database;
	}

	public static IDatabase activate(DatabaseConfig config) {
		try {
			var db = config.connect(Workspace.dbDir());
			setActive(config, db);
			LoggerFactory.getLogger(Database.class)
					.info("activated database {} with version{}",
							db.getName(), db.getVersion());
			// setting the active database may fail, the global
			// database variable is then null and this is what we
			// return here
			return database;
		} catch (Exception e) {
			try {
				close();
			} catch (Exception ce) {
				LoggerFactory.getLogger(Database.class)
						.error("failed to close database resources", ce);
			}
			ErrorReporter.on("failed to activate database: " + config, e);
			return null;
		}
	}

	public static void setActive(DatabaseConfig config, IDatabase db) {
		try {
			database = db;
			Database.config = config;
			Cache.create(database);
			Repository.open(Repository.gitDir(database.getName()), database);
			RcpWindowAdvisor.updateWindowTitle();
		} catch (RuntimeException e) {
			if (Repository.CURRENT != null) {
				Repository.CURRENT.close();
			}
			Cache.close();
			database = null;
			Database.config = null;
			throw e;
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
		try {
			Cache.close();
			CopyPaste.clearCache();
			if (Repository.CURRENT != null) {
				Repository.CURRENT.close();
			}
			database.close();
			database = null;
			config = null;
			RcpWindowAdvisor.updateWindowTitle();
		} catch (RuntimeException e){
			// if an error occurs we still reset these globals
			config = null;
			database = null;
		}
	}

	private static DatabaseConfigList readConfigs() {
		var file = new File(Workspace.root(), "databases.json");
		if (!file.exists())
			return new DatabaseConfigList();

		var configs = DatabaseConfigList.read(file);

		// sync local databases with workspace; the databases.json file
		// could be out-of-sync with the databases folder
		var found = new HashSet<String>();
		for (var local : configs.getDerbyConfigs()) {
			var dbDir = DatabaseDir.getRootFolder(local.name());
			found.add(local.name());
			if (!dbDir.exists()) {
				// we do not delete the config currently
				LoggerFactory.getLogger(Database.class).warn(
						"registered database '{}' does not exist in workspace",
						local.name());
			}
		}

		// add configs for non-registered databases
		var dbDirs = Workspace.dbDir().listFiles();
		var updated = false;
		if (dbDirs != null) {
			for (var dbDir : dbDirs) {
				var name = dbDir.getName();
				if (!found.contains(name) && Derby.isDerbyFolder(dbDir)) {
					var config = new DerbyConfig().name(name);
					configs.getDerbyConfigs().add(config);
					updated = true;
				}
			}
		}

		if (updated) {
			configs.write(file);
		}

		return configs;
	}

	public static void saveConfig() {
		var file = new File(Workspace.root(), "databases.json");
		configurations.write(file);
	}

	public static DatabaseConfigList getConfigurations() {
		return configurations;
	}

	public static DatabaseConfig getActiveConfiguration() {
		for (var conf : configurations.getAll()) {
			if (isActive(conf))
				return conf;
		}
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
		if (name == null || name.isBlank() || name.isEmpty())
			return "An empty name is not allowed.";
		if (!name.strip().equals(name)) {
			return "The given database name contains" +
					" leading or trailing whitespaces.";
		}
		try {
			var dir = new File(Workspace.dbDir(), name);
			if (dir.exists())
				return M.NewDatabase_AlreadyExists;
			Paths.get(dir.getAbsolutePath());
		} catch (Exception e) {
			return M.NewDatabase_InvalidName;
		}
		return getConfigurations().nameExists(name)
				? M.NewDatabase_AlreadyExists
				: null;
	}
}
