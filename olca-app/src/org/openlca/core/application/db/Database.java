package org.openlca.core.application.db;

import java.io.File;
import java.util.Objects;

import javax.persistence.EntityManagerFactory;

import org.openlca.core.application.App;
import org.openlca.core.database.ActorDao;
import org.openlca.core.database.BaseDao;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.FlowPropertyDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.IRootEntityDao;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.database.ProductSystemDao;
import org.openlca.core.database.ProjectDao;
import org.openlca.core.database.SourceDao;
import org.openlca.core.database.UnitGroupDao;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;

/** Database management of the application. */
public class Database {

	private static IDatabase database;
	private static IDatabaseConfiguration config;
	private static DatabaseList configurations = loadConfigs();

	private Database() {
	}

	public static IDatabase get() {
		return database;
	}

	@SuppressWarnings("unchecked")
	public static <T> T load(BaseDescriptor descriptor) throws Exception {
		if (descriptor == null || descriptor.getModelType() == null)
			return null;
		Class<?> clazz = descriptor.getModelType().getModelClass();
		Object o = createDao(clazz).getForId(descriptor.getId());
		return (T) o;
	}

	public static EntityManagerFactory getEntityFactory() {
		if (database != null)
			return database.getEntityFactory();
		else
			return null;
	}

	public static IDatabase activate(IDatabaseConfiguration config)
			throws Exception {
		Database.database = config.createInstance();
		Database.config = config;
		return Database.database;
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
		database.close();
		database = null;
		config = null;
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

	public static <T> BaseDao<T> createDao(Class<T> clazz) {
		if (database == null)
			return null;
		else
			return database.createDao(clazz);
	}

	public static IRootEntityDao<?> createRootDao(ModelType type) {
		if (database == null)
			return null;
		switch (type) {
		case ACTOR:
			return new ActorDao(getEntityFactory());
		case FLOW:
			return new FlowDao(getEntityFactory());
		case FLOW_PROPERTY:
			return new FlowPropertyDao(getEntityFactory());
		case IMPACT_METHOD:
			return new ImpactMethodDao(getEntityFactory());
		case PROCESS:
			return new ImpactMethodDao(getEntityFactory());
		case PRODUCT_SYSTEM:
			return new ProductSystemDao(getEntityFactory());
		case PROJECT:
			return new ProjectDao(getEntityFactory());
		case SOURCE:
			return new SourceDao(getEntityFactory());
		case UNIT_GROUP:
			return new UnitGroupDao(getEntityFactory());
		default:
			return null;
		}
	}

}
