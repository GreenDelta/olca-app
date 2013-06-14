package org.openlca.core.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

import org.openlca.core.database.IDatabaseServer;
import org.openlca.util.Strings;

/** Data of a MySQL connection. */
public final class ConnectionData {

	private String databaseName;
	private String host;
	private String port;
	private String password;
	private String user;

	/** Default constructor; clients should use the respective setters. */
	public ConnectionData() {
	}

	/** Initializes the connection data using the given properties. */
	public ConnectionData(Map<String, String> properties) {
		databaseName = properties.get(IDatabaseServer.DATABASE);
		host = properties.get(IDatabaseServer.HOST);
		port = properties.get(IDatabaseServer.PORT);
		password = properties.get(IDatabaseServer.PASSWORD);
		user = properties.get(IDatabaseServer.USER);
	}

	/** Copies the fields from the given data into the new instance. */
	public ConnectionData(ConnectionData other) {
		databaseName = other.databaseName;
		host = other.host;
		port = other.port;
		password = other.password;
		user = other.user;
	}

	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public String getHost() {
		return host;
	}

	public String getPort() {
		return port;
	}

	public String getPassword() {
		return password;
	}

	public String getUser() {
		return user;
	}

	/** Creates a new connection to a database server using the connection data. */
	public Connection createServerConnection() throws SQLException {
		String url = "jdbc:mysql://" + host + ":" + port;
		return DriverManager.getConnection(url, user, password);
	}

	/** Creates a new connection to a database using the connection data. */
	public Connection createDatabaseConnection() throws SQLException {
		String url = "jdbc:mysql://" + host + ":" + port + "/" + databaseName;
		return DriverManager.getConnection(url, user, password);
	}

	@Override
	public String toString() {
		if (databaseName != null)
			return "jdbc:mysql://" + host + ":" + port + "/" + databaseName;
		return "jdbc:mysql://" + host + ":" + port;
	}

	@Override
	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (this == other)
			return true;
		if (!(other instanceof ConnectionData))
			return false;
		ConnectionData otherData = (ConnectionData) other;
		return this.toString().equals(other.toString())
				&& Strings.nullOrEqual(user, otherData.user)
				&& Strings.nullOrEqual(password, otherData.password);
	}

	@Override
	public int hashCode() {
		String s = this.toString() + user + password;
		return s.hashCode();
	}

}
