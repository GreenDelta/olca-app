package org.openlca.app.db;

import org.openlca.app.App;
import org.openlca.core.database.IDatabase;

import java.io.File;

/**
 * Contains helper methods for database folders. The folder of a database is
 * located under <workspace>/databases/<database_name>. Even remote databases
 * can have such folders for storing additional files.
 * <p>
 * Note that the getter methods in this class do not create the file directories
 * in the getter methods.
 */
public class DatabaseFolder {

	private DatabaseFolder() {
	}

	public static File getRootFolder(String databaseName) {
		File workspace = App.getWorkspace();
		File dbFolder = new File(workspace, "databases");
		return new File(dbFolder, databaseName);
	}

	/**
	 * Get the general location for storing additional files for a database
	 * (in general this is database_name/_olca_)
	 */
	public static File getFileStorageLocation(IDatabase database) {
		if (database.getFileStorageLocation() != null)
			return database.getFileStorageLocation();
		else
			return new File(getRootFolder(database.getName()), "_olca_");
	}

	/**
	 * Shapefiles are stored per LCIA method under the folder
	 * database/shapefiles/method_uuid.
	 */
	public static File getShapeFileLocation(IDatabase database, String methodUuid) {
		File fileStore = getFileStorageLocation(database);
		File shapeDir = new File(fileStore, "shapefiles");
		return new File(shapeDir, methodUuid);
	}

	public static File getExternalDocLocation(IDatabase database) {
		return new File(getFileStorageLocation(database), "external_docs");
	}

}
