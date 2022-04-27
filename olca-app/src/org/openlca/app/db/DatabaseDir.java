package org.openlca.app.db;

import java.io.File;

import org.openlca.app.rcp.Workspace;
import org.openlca.core.database.FileStore;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.descriptors.Descriptor;

/**
 * Contains helper methods for database folders. The folder of a database is
 * located under <workspace>/databases/<database_name>. Even remote databases
 * can have such folders for storing additional files.
 * <p>
 * Note that the getter methods in this class do not create the file directories
 * in the getter methods.
 */
public class DatabaseDir {

	public static final String FILE_STORAGE = "_olca_";

	private DatabaseDir() {
	}

	public static File getRootFolder(String databaseName) {
		return new File(Workspace.dbDir(), databaseName);
	}

	/**
	 * Get the general location for storing additional files for a database (in
	 * general this is database_name/_olca_)
	 */
	static File getFileStorageLocation(IDatabase db) {
		if (db.getFileStorageLocation() != null)
			return db.getFileStorageLocation();
		else
			return new File(getRootFolder(db.getName()), FILE_STORAGE);
	}

	public static File getDir(Descriptor d) {
		File root = getFileStorageLocation(Database.get());
		FileStore fs = new FileStore(root);
		return fs.getFolder(d);
	}

	public static File getDir(RootEntity e) {
		File root = getFileStorageLocation(Database.get());
		FileStore fs = new FileStore(root);
		return fs.getFolder(e);
	}

	public static void deleteDir(Descriptor d) {
		File dir = DatabaseDir.getFileStorageLocation(Database.get());
		if (dir == null || !dir.exists())
			return;
		FileStore fs = new FileStore(dir);
		fs.deleteFolder(d);
	}

	public static void copyDir(RootEntity from, RootEntity to) {
		File dir = DatabaseDir.getFileStorageLocation(Database.get());
		if (dir == null || !dir.exists())
			return;
		FileStore fs = new FileStore(dir);
		fs.copyFolder(from, to);
	}

}
