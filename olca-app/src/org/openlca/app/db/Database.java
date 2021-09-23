package org.openlca.app.db;

import java.io.File;
import java.util.Objects;

import org.openlca.app.App;
import org.openlca.app.cloud.TokenDialog;
import org.openlca.app.cloud.index.DiffIndex;
import org.openlca.app.navigation.CopyPaste;
import org.openlca.app.util.ErrorReporter;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.api.RepositoryConfig;
import org.openlca.cloud.api.update.RepositoryConfigConversion;
import org.openlca.core.DataDir;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.config.DatabaseConfig;
import org.openlca.core.database.config.DatabaseConfigList;
import org.openlca.core.database.config.DerbyConfig;
import org.openlca.core.database.config.MySqlConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Database management of the application. */
public class Database {

	private static IDatabase database;
	private static DatabaseConfig config;
	private static DatabaseListener listener;
	private static final DatabaseConfigList configurations = loadConfigs();
	private static DiffIndex diffIndex;
	private static RepositoryClient repositoryClient;

	private Database() {
	}

	public static IDatabase get() {
		return database;
	}

	public static IndexUpdater getIndexUpdater() {
		return listener.getIndexUpdater();
	}

	public static IDatabase activate(DatabaseConfig config) {
		try {
			database = config.connect(DataDir.databases());
			listener = new DatabaseListener(Database.database);
			database.addListener(listener);
			Cache.create(database);
			Database.config = config;
			Logger log = LoggerFactory.getLogger(Database.class);
			log.trace("activated database {} with version{}",
					database.getName(), database.getVersion());
			tryConnectRepository();
			return database;
		} catch (Exception e) {
			database = null;
			Cache.close();
			Database.config = null;
			ErrorReporter.on("failed to activate database: " + config, e);
			return null;
		}
	}

	private static void tryConnectRepository() {
		try {
			if (RepositoryConfigConversion.needsConversion(database)) {
				RepositoryConfigConversion.applyTo(database);
			}
			var repoConfig = RepositoryConfig.loadActive(Database.get());
			if (repoConfig != null) {
				repoConfig.credentials.setTokenSupplier(TokenDialog::prompt);
				connect(new RepositoryClient(repoConfig));
			}
		} catch (Exception e) {
			disconnect();
		}
	}

	public static void connect(RepositoryClient client) {
		if (diffIndex != null)
			diffIndex.close();
		repositoryClient = client;
		diffIndex = DiffIndex.getFor(repositoryClient);
	}

	public static void disconnect() {
		if (repositoryClient == null)
			return;
		diffIndex.close();
		diffIndex = null;
		repositoryClient = null;
	}

	public static boolean isConnected() {
		return repositoryClient != null;
	}

	public static RepositoryClient getRepositoryClient() {
		return repositoryClient;
	}

	public static DiffIndex getDiffIndex() {
		return diffIndex;
	}

	public static boolean isActive(DatabaseConfig config) {
		if (config == null)
			return false;
		return Objects.equals(config, Database.config);
	}

	/** Closes the active database. */
	public static void close() throws Exception {
		if (database == null)
			return;
		Cache.close();
		CopyPaste.clearCache();
		database.close();
		database = null;
		listener = null;
		config = null;
		if (repositoryClient == null)
			return;
		diffIndex.close();
		diffIndex = null;
		repositoryClient = null;
	}

	private static DatabaseConfigList loadConfigs() {
		var workspace = App.getWorkspace();
		var listFile = new File(workspace, "databases.json");
		return !listFile.exists()
			? new DatabaseConfigList()
			: DatabaseConfigList.read(listFile);
	}

	private static void saveConfig() {
		File workspace = App.getWorkspace();
		File listFile = new File(workspace, "databases.json");
		configurations.write(listFile);
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

}
