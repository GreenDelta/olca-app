package org.openlca.app.db;

import java.io.File;
import java.util.Objects;

import org.openlca.core.database.Derby;

/**
 * Configuration of a local derby database. Derby databases are stored directly
 * in the workspace folder: <workspace>/databases/<database_name>.
 */
public class DerbyConfiguration implements DatabaseConfig {

	private String name;

	/**
	 * experimental: An optional full path to the directory that contains the
	 * database. This is currently not an option provided by the user interface.
	 */
	private String folder;

	@Override
	public Derby connect() {
		return folder != null
				? new Derby(new File(folder))
				: new Derby(DatabaseDir.getRootFolder(name));
	}

	@Override
	public String name() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public boolean isEmbedded() {
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
