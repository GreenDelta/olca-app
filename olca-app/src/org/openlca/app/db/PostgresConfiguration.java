package org.openlca.app.db;

import java.util.Objects;

import org.openlca.core.database.IDatabase;
import org.openlca.core.database.postgres.PostgresConnParams;
import org.openlca.core.database.postgres.PostgresDatabase;

/**
 * Configuration of a Postgres database.
 */
public class PostgresConfiguration implements IDatabaseConfiguration {

	private String name;
	private String host;
	private int port;
	private String user;
	private String password;

	public IDatabase createInstance() throws Exception {
	    Class.forName("org.postgresql.Driver");
	    PostgresConnParams connParams = new PostgresConnParams(host, ""+port, name, user, password);
		PostgresDatabase db = new PostgresDatabase(connParams);
		db.setFileStorageLocation(DatabaseDir.getFileStorageLocation(db));
		return db;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!getClass().equals(obj.getClass()))
			return false;
		PostgresConfiguration other = (PostgresConfiguration) obj;
		return Objects.equals(this.host, other.host)
				&& Objects.equals(this.port, other.port)
				&& Objects.equals(this.name, other.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(host, port, name);
	}

	@Override
	public boolean isLocal() {
		return false;
	}

}
