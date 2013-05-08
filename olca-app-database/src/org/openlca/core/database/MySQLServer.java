/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Mozilla Public License v1.1
 * which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 *
 * Contributors:
 *     	GreenDeltaTC - initial API and implementation
 *		www.greendeltatc.com
 *		tel.:  +49 30 4849 6030
 *		mail:  gdtc@greendeltatc.com
 *******************************************************************************/

package org.openlca.core.database;

import java.io.File;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openlca.core.database.upgrades.Updates;
import org.openlca.core.jobs.JobHandler;
import org.openlca.core.jobs.Jobs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysql.jdbc.Driver;

/**
 * Implementation of the {@link IDatabaseServer} interface with a MySQL Server
 * as data source.
 * 
 */
public class MySQLServer implements IDatabaseServer {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private ServerApp serverApp;
	private JobHandler handler = Jobs.getHandler(Jobs.MAIN_JOB_HANDLER);
	private boolean running = false;
	private Map<String, String> properties = new HashMap<>();
	private List<IDatabase> connections = new ArrayList<>();

	public void setServerApp(ServerApp serverApp) {
		this.serverApp = serverApp;
	}

	@Override
	public IDatabase createDatabase(String name, int contentType)
			throws Exception {
		ConnectionData data = new ConnectionData(properties);
		data.setDatabaseName(name);
		MySQLDatabase database = new MySQLDatabase(data);
		database.createEmpty();
		database.execute("current_schema.sql");
		if (contentType == IDatabaseServer.CONTENT_TYPE_ALL_REF)
			database.execute("ref_data_all.sql");
		else if (contentType == IDatabaseServer.CONTENT_TYPE_UNITS)
			database.execute("ref_data_units.sql");
		database.connect();
		connections.add(database);
		return database;
	}

	@Override
	public void delete(IDatabase database) throws Exception {
		if (!(database instanceof MySQLDatabase))
			return;
		log.info("Delete database {}", database);
		MySQLDatabase myDb = (MySQLDatabase) database;
		try {
			myDb.drop();
			connections.remove(myDb);
		} catch (Exception e) {
			log.error("Could not delete database", e);
		}
	}

	@Override
	public String getName(final boolean includeProperties) {
		String name = "MySQL";
		if (includeProperties) {
			name += " at " + properties.get(IDatabaseServer.HOST) + ":"
					+ properties.get(IDatabaseServer.PORT);
		}
		return name;
	}

	@Override
	public Map<String, String> getProperties() {
		if (properties == null)
			return Collections.emptyMap();
		return properties;
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
		this.properties.put(IDatabaseServer.DRIVER_CLASS,
				Driver.class.getCanonicalName());
	}

	@Override
	public void shutdown() {
		if (serverApp != null) {
			try {
				serverApp.shutdown();
				serverApp = null;
			} catch (Exception e) {
				log.error("Could not shutdown embedded server", e);
			}
		}
		for (IDatabase db : connections) {
			log.trace("Close database {}", db.getName());
			db.getEntityFactory().close();
		}
		connections.clear();
		running = false;
	}

	@Override
	public void connect() throws Exception {
		log.trace("Start database server.");
		handler.startJob(
				org.openlca.core.database.Messages.StartingEmbeddedServer, -1);
		try {
			DriverManager.registerDriver(new Driver());
			running = true;
		} catch (Exception e) {
			log.error("Could not start embedded MySQL server");
			running = false;
			throw e;
		} finally {
			handler.done();
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (obj instanceof MySQLServer) {
			MySQLServer other = (MySQLServer) obj;
			return this.properties != null && other.properties != null
					&& this.properties.equals(other.properties);
		}
		return false;
	}

	@Override
	public int hashCode() {
		if (properties != null)
			return properties.hashCode();
		return super.hashCode();
	}

	@Override
	public IDatabase importDatabase(String dbName, File packageFile)
			throws DataProviderException {
		log.info("Import database: {}", dbName);
		ConnectionData data = new ConnectionData(properties);
		data.setDatabaseName(dbName);
		MySQLDatabaseImport databaseImport = new MySQLDatabaseImport(data,
				packageFile);
		MySQLDatabase db = databaseImport.run();
		db.connect();
		connections.add(db);
		return db;
	}

	@Override
	public void exportDatabase(IDatabase database, File toScriptFile)
			throws DataProviderException {
		if (!(database instanceof MySQLDatabase))
			throw new DataProviderException(
					"The given database is not a MySQL database");
		MySQLDatabase db = (MySQLDatabase) database;
		log.info("Export database {} ", db.getName());
		handler.startJob("Packing file", -1);
		handler.subJob("");
		ConnectionData data = new ConnectionData(properties);
		data.setDatabaseName(database.getName());
		MySQLDatabaseExport databaseExport = new MySQLDatabaseExport(data,
				toScriptFile);
		databaseExport.run();
		handler.done();
	}

	@Override
	public List<DatabaseDescriptor> getDatabaseDescriptors() {
		ConnectionData data = new ConnectionData(properties);
		DescriptorFetch fetch = new DescriptorFetch(data);
		return fetch.doFetch();
	}

	@Override
	public List<IDatabase> getConnectedDatabases() {
		return connections;
	}

	@Override
	public IDatabase connect(DatabaseDescriptor descriptor) throws Exception {
		if (descriptor == null)
			return null;
		log.trace("Connect to database {}", descriptor);
		ConnectionData data = new ConnectionData(properties);
		data.setDatabaseName(descriptor.getName());
		MySQLDatabase db = new MySQLDatabase(data);
		db.connect();
		connections.add(db);
		return db;
	}

	@Override
	public void update(DatabaseDescriptor descriptor) throws Exception {
		ConnectionData data = new ConnectionData(properties);
		data.setDatabaseName(descriptor.getName());
		Updates.checkAndRun(data);
		descriptor.setVersion(Version.CURRENT);
		descriptor.setUpToDate(true);
	}

	@Override
	public boolean isEmbedded() {
		return "true".equals(properties.get(IDatabaseServer.EMBEDDED));
	}
}
