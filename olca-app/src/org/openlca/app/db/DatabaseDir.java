package org.openlca.app.db;

import java.io.File;

import org.openlca.app.App;
import org.openlca.core.database.FileStore;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.descriptors.BaseDescriptor;

/**
 * Contains helper methods for database folders. The folder of a database is
 * located under <workspace>/databases/<database_name>. Even remote databases
 * can have such folders for storing additional files.
 * <p>
 * Note that the getter methods in this class do not create the file directories
 * in the getter methods.
 */
public class DatabaseDir {

	private DatabaseDir() {
	}

	public static File getRootFolder(String databaseName) {
		File workspace = App.getWorkspace();
		File dbFolder = new File(workspace, "databases");
		return new File(dbFolder, databaseName);
	}

	/**
	 * Get the general location for storing additional files for a database (in
	 * general this is database_name/_olca_)
	 */
	public static File getFileStorageLocation(IDatabase database) {
		if (database.getFileStorageLocation() != null)
			return database.getFileStorageLocation();
		else
			return new File(getRootFolder(database.getName()), "_olca_");
	}

	public static void deleteDir(BaseDescriptor descriptor) {
		File dir = DatabaseDir.getFileStorageLocation(Database.get());
		if (dir == null || !dir.exists())
			return;
		FileStore fs = new FileStore(dir);
		fs.deleteFolder(descriptor);
	}

	public static void copyDir(CategorizedEntity from, CategorizedEntity to) {
		File dir = DatabaseDir.getFileStorageLocation(Database.get());
		if (dir == null || !dir.exists())
			return;
		FileStore fs = new FileStore(dir);
		fs.copyFolder(from, to);
	}

}
