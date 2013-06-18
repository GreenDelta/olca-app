package org.openlca.core.application.db;

import java.io.File;
import java.util.Objects;

import org.openlca.core.database.IDatabase;

/**
 * Configuration of a derby database.
 */
public class DerbyConfiguration implements IDatabaseConfiguration {

	private File folder;
	private String name;

	public IDatabase createInstance() throws Exception {
		// TODO not yet implemented
		return null;
	}

	public File getFolder() {
		return folder;
	}

	public void setFolder(File folder) {
		this.folder = folder;
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
		return Objects.equals(this.folder, other.folder)
				&& Objects.equals(this.name, other.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(folder, name);
	}

}
