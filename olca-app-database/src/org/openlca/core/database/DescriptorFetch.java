package org.openlca.core.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.core.database.DatabaseDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Get the database descriptors from a server connection.
 */
class DescriptorFetch {

	private Logger log = LoggerFactory.getLogger(getClass());
	private ConnectionData data;

	public DescriptorFetch(ConnectionData data) {
		this.data = data;
	}

	public List<DatabaseDescriptor> doFetch() {
		log.trace("Get database descriptors from {}", data);
		String query = "SHOW DATABASES";
		try (Connection con = data.createServerConnection();
				ResultSet set = con.createStatement().executeQuery(query)) {
			List<DatabaseDescriptor> descriptors = new ArrayList<>();
			while (set.next()) {
				String databaseName = set.getString(1);
				DatabaseDescriptor descriptor = getDescriptor(databaseName);
				if (descriptor != null)
					descriptors.add(descriptor);
			}
			return descriptors;
		} catch (Exception e) {
			log.error("Failed to get database descriptors", e);
			return Collections.emptyList();
		}
	}

	private DatabaseDescriptor getDescriptor(String databaseName) {
		log.trace("Check database {}", databaseName);
		ConnectionData dbData = new ConnectionData(data);
		dbData.setDatabaseName(databaseName);
		try {
			if (!hasVersionTable(dbData))
				return null;
			String version = getVersion(dbData);
			DatabaseDescriptor descriptor = new DatabaseDescriptor();
			descriptor.setName(databaseName);
			descriptor.setVersion(version);
			descriptor.setUpToDate(Version.CURRENT.equals(version));
			return descriptor;
		} catch (Exception e) {
			log.error("Failed to check database " + databaseName, e);
			return null;
		}
	}

	private boolean hasVersionTable(ConnectionData dbData) throws Exception {
		String query = "SHOW TABLES";
		try (Connection con = dbData.createDatabaseConnection();
				ResultSet rs = con.createStatement().executeQuery(query)) {
			boolean found = false;
			while (!found && rs.next())
				found = Version.TABLE.equals(rs.getString(1));
			return found;
		}
	}

	private String getVersion(ConnectionData dbData) throws Exception {
		String query = "SELECT " + Version.FIELD + " FROM " + Version.TABLE;
		try (Connection con = dbData.createDatabaseConnection();
				ResultSet rs = con.createStatement().executeQuery(query)) {
			rs.first();
			return rs.getString(Version.FIELD);
		}
	}

}
