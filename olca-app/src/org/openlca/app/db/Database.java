package org.openlca.app.db;

import java.io.File;
import java.util.Objects;

import org.openlca.app.App;
import org.openlca.app.cloud.TokenDialog;
import org.openlca.app.cloud.index.DiffIndex;
import org.openlca.app.navigation.CopyPaste;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.api.RepositoryConfig;
import org.openlca.core.database.IDatabase;
import org.openlca.ipc.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Database management of the application. */
public class Database {

	private static IDatabase database;
	private static IDatabaseConfiguration config;
	private static DatabaseListener listener;
	private static DatabaseList configurations = loadConfigs();
	private static DiffIndex diffIndex;
	private static RepositoryClient repositoryClient;
	private static Server ipcServer;

	private Database() {
	}

	public static IDatabase get() {
		return database;
	}

	public static IndexUpdater getIndexUpdater() {
		return listener.getIndexUpdater();
	}

	public static IDatabase activate(IDatabaseConfiguration config)
			throws Exception {
		try {
			Database.database = config.createInstance();
			listener = new DatabaseListener(Database.database);
			Database.database.addListener(listener);
			Cache.create(database);
			Database.config = config;
			Logger log = LoggerFactory.getLogger(Database.class);
			log.trace("activated database {} with version{}",
					database.getName(), database.getVersion());
			RepositoryConfig repoConfig = RepositoryConfig.loadFor(Database.get());
			if (repoConfig != null) {
				repoConfig.credentials.setTokenSupplier(TokenDialog::prompt);
				connect(new RepositoryClient(repoConfig));
			}
			return Database.database;
		} catch (Exception e) {
			Database.database = null;
			Cache.close();
			Database.config = null;
			throw e;
		}
	}

	public static void startIpcOn(int port) {
		ipcServer = new Server(port);
		ipcServer.withDefaultHandlers(get(), App.getSolver());
		ipcServer.start();
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
		repositoryClient.getConfig().disconnect();
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

	public static boolean isActive(IDatabaseConfiguration config) {
		if (config == null)
			return false;
		return Objects.equals(config, Database.config);
	}

	/** Closes the active database. */
	public static void close() throws Exception {
		if (database == null)
			return;
		if (ipcServer != null) {
			ipcServer.stop();
			ipcServer = null;
		}
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

	private static DatabaseList loadConfigs() {
		File workspace = App.getWorkspace();
		File listFile = new File(workspace, "databases.json");
		if (!listFile.exists())
			return new DatabaseList();
		else
			return DatabaseList.read(listFile);
	}

	private static void saveConfig() {
		File workspace = App.getWorkspace();
		File listFile = new File(workspace, "databases.json");
		configurations.write(listFile);
	}

	public static DatabaseList getConfigurations() {
		return configurations;
	}

	public static IDatabaseConfiguration getActiveConfiguration() {
		for (IDatabaseConfiguration conf : configurations.getLocalDatabases())
			if (isActive(conf))
				return conf;
		for (IDatabaseConfiguration conf : configurations.getRemoteDatabases())
			if (isActive(conf))
				return conf;
		return null;
	}

	public static void register(DerbyConfiguration config) {
		if (configurations.contains(config))
			return;
		configurations.getLocalDatabases().add(config);
		saveConfig();
	}

	public static void remove(DerbyConfiguration config) {
		if (!configurations.contains(config))
			return;
		configurations.getLocalDatabases().remove(config);
		saveConfig();
	}

	public static void register(MySQLConfiguration config) {
		if (configurations.contains(config))
			return;
		configurations.getRemoteDatabases().add(config);
		saveConfig();
	}

	public static void remove(MySQLConfiguration config) {
		if (!configurations.contains(config))
			return;
		configurations.getRemoteDatabases().remove(config);
		saveConfig();
	}

}
