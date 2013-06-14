package org.openlca.core.database;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;

import org.openlca.core.database.JPADatabase;
import org.openlca.core.database.util.ScriptRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IDatabase implementation for MySQL database.
 */
public class MySQLDatabase extends JPADatabase {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private ConnectionData connectionData;
	private EntityManagerFactory entityFactory;

	public MySQLDatabase(ConnectionData data) {
		this.connectionData = data;
	}

	@Override
	public EntityManagerFactory getEntityFactory() {
		return entityFactory;
	}

	/** Creates the entity manager factor for the database. */
	public void connect() {
		if (entityFactory != null && entityFactory.isOpen())
			return;
		String url = "jdbc:mysql://" + connectionData.getHost() + ":"
				+ connectionData.getPort() + "/"
				+ connectionData.getDatabaseName();
		Map<Object, Object> map = new HashMap<>();
		map.put(PersistenceUnitProperties.JDBC_URL, url);
		map.put(PersistenceUnitProperties.JDBC_USER, connectionData.getUser());
		map.put(PersistenceUnitProperties.JDBC_PASSWORD,
				connectionData.getPassword());
		map.put(PersistenceUnitProperties.JDBC_DRIVER, "com.mysql.jdbc.Driver");
		map.put(PersistenceUnitProperties.CLASSLOADER, getClass()
				.getClassLoader());
		map.put(PersistenceUnitProperties.TARGET_DATABASE, "MySQL");
		entityFactory = new PersistenceProvider().createEntityManagerFactory(
				"openLCA", map);
	}

	/** Creates the database without any tables. */
	public void createEmpty() throws Exception {
		log.info("Create empty database {}", connectionData);
		String stmt = "CREATE DATABASE " + connectionData.getDatabaseName();
		try (Connection con = connectionData.createServerConnection()) {
			con.createStatement().execute(stmt);
		} catch (Exception e) {
			log.error("Failed to create database", e);
			throw e;
		}
	}

	/** Drops the database and closes the entity manager factory. */
	public void drop() throws SQLException {
		log.info("close persistence context");
		getEntityFactory().close();
		log.info("delete database {}", connectionData);
		String stmt = "DROP DATABASE " + connectionData.getDatabaseName();
		try (Connection con = connectionData.createServerConnection()) {
			con.createStatement().execute(stmt);
		}
	}

	/** Executes the SQL script resource with the given name. */
	public void execute(String script) throws Exception {
		log.trace("Execute script file {}", script);
		try (InputStream stream = getClass().getResourceAsStream(script)) {
			execute(stream, "utf-8");
		}
	}

	/** Executes the SQL statements from the given stream. */
	public void execute(InputStream stream, String encoding) throws Exception {
		ScriptRunner scriptRunner = new ScriptRunner(connectionData);
		scriptRunner.run(stream, encoding);
		stream.close();
	}

	@Override
	public Connection createConnection() {
		try {
			return connectionData.createDatabaseConnection();
		} catch (Exception e) {
			log.error("Failed to create database connection", e);
			return null;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (obj instanceof MySQLDatabase) {
			MySQLDatabase other = (MySQLDatabase) obj;
			return this.connectionData.equals(other.connectionData);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return connectionData.hashCode();
	}

	@Override
	public String getName() {
		return connectionData.getDatabaseName();
	}

	@Override
	public String getUrl() {
		return "jdbc:mysql://" + connectionData.getHost() + ":"
				+ connectionData.getPort() + "/"
				+ connectionData.getDatabaseName();
	}

}
