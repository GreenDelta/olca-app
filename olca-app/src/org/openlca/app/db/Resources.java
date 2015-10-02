package org.openlca.app.db;

import java.io.File;

import org.openlca.core.database.FileStore;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.descriptors.BaseDescriptor;

/**
 * Provides helper methods for the management of external resources of entities
 * (like shape-files for LCIA methods or layout files of product systems).
 */
public class Resources {

	public static void delete(BaseDescriptor descriptor) {
		File dir = DatabaseFolder.getFileStorageLocation(Database.get());
		if (dir == null || !dir.exists())
			return;
		FileStore fs = new FileStore(dir);
		fs.deleteFolder(descriptor);
	}

	public static void copy(CategorizedEntity from, CategorizedEntity to) {
		File dir = DatabaseFolder.getFileStorageLocation(Database.get());
		if (dir == null || !dir.exists())
			return;
		FileStore fs = new FileStore(dir);
		fs.copyFolder(from, to);
	}

}
