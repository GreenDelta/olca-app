package org.openlca.app.db;

import java.io.File;
import java.util.Objects;

import org.openlca.core.database.Derby;

/**
 * Configuration of an embedded Derby database.
 */
public class DerbyConfig implements DatabaseConfig {

	private String name;
	private String folder;

	@Override
	public Derby connect(File databasesDir) {
		return folder != null
			? new Derby(new File(folder))
			: new Derby(new File(databasesDir, name));
	}

	@Override
	public String name() {
		if (name == null)
			return folder != null
				? new File(folder).getName()
				: null;
		return name;
	}

	public DerbyConfig name(String name) {
		this.name = Objects.requireNonNull(name);
		return this;
	}

	/**
	 * Optionally sets a full path to the database folder.
	 *
	 * @param folder a path to the databases' folder
	 * @return the instance of this configuration
	 */
	public DerbyConfig folder(String folder) {
		this.folder = Objects.requireNonNull(folder);
		return this;
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
		DerbyConfig other = (DerbyConfig) obj;
		return Objects.equals(this.name, other.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

}
