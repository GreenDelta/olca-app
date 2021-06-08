package org.openlca.app.db;

import java.io.File;
import java.util.Objects;

import org.openlca.core.database.IDatabase;
import org.openlca.core.database.MySQL;

/**
 * Stores the connection configuration of a MySQL database.
 */
public final class MySqlConfig implements DatabaseConfig {

	private String name = "openlca";
	private String host = "localhost";
	private int port = 3306;
	private String user = "root";
	private String password;

	@Override
	public IDatabase connect(File databasesDir) {
		var db = MySQL.database(name)
			.host(host)
			.port(port)
			.user(user)
			.password(password)
			.connect();
		db.setFileStorageLocation(new File(databasesDir, name));
		return db;
	}

	@Override
	public String name() {
		return name;
	}

	public MySqlConfig name(String name) {
		this.name = Objects.requireNonNull(name);
		return this;
	}

	public String host() {
		return host;
	}

	public MySqlConfig host(String host) {
		this.host = Objects.requireNonNull(host);
		return this;
	}

	public int port() {
		return port;
	}

	public MySqlConfig port(int port) {
		this.port = port;
		return this;
	}

	public String user() {
		return user;
	}

	public MySqlConfig user(String user) {
		this.user = Objects.requireNonNull(user);
		return this;
	}

	public String password() {
		return password;
	}

	public MySqlConfig password(String password) {
		this.password = password;
		return this;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!getClass().equals(obj.getClass()))
			return false;
		MySqlConfig other = (MySqlConfig) obj;
		return Objects.equals(this.host, other.host)
				&& Objects.equals(this.port, other.port)
				&& Objects.equals(this.name, other.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(host, port, name);
	}

	@Override
	public boolean isEmbedded() {
		return false;
	}

}
