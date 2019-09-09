package org.openlca.app.db;

import java.util.Objects;

import org.openlca.core.database.derby.DerbyDatabase;

/**
 * Configuration of a local derby database. Derby databases are stored directly
 * in the workspace folder: <workspace>/databases/<database_name>.
 */
public class DerbyConfiguration implements ILocalDatabaseConfiguration {

	private String name;

	@Override
	public DerbyDatabase createInstance() throws Exception {
		DerbyDatabase db = new DerbyDatabase(DatabaseDir.getRootFolder(name));
		return db;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public boolean isLocal() {
		return true;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!getClass().equals(obj.getClass()))
			return false;
		DerbyConfiguration other = (DerbyConfiguration) obj;
		return Objects.equals(this.name, other.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

}
