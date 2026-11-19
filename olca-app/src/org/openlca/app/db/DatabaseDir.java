package org.openlca.app.db;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.openlca.app.rcp.Workspace;
import org.openlca.commons.Res;
import org.openlca.core.database.FileStore;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.util.Dirs;
import org.slf4j.LoggerFactory;

/// Contains helper methods for database folders. The folder of a database is
/// located under `<workspace>/databases/<database_name>`. Even remote
/// databases can have such folders for storing additional files.
///
/// Note that the getter methods in this class do not create the file directories
/// in the getter methods.
public class DatabaseDir {

	public static final String FILE_STORAGE = "_olca_";

	/// The name prefix of database folders that are marked for deletion, see
	/// `moveToTrash`.
	private static final String TRASH_PREFIX = ".deleted-";

	private DatabaseDir() {
	}

	public static File getRootFolder(String databaseName) {
		return new File(Workspace.dbDir(), databaseName);
	}

	/// Marks the given database folder for deletion by renaming it to a trash
	/// folder in the same parent folder, which is returned. On Windows, this
	/// rename fails when another program holds a file in that folder open. So
	/// when this method succeeds, the deletion of the folder is unlikely to fail;
	/// and when it fails, the database is still complete. A trash folder is never
	/// registered as a database (see `isTrash`); if its deletion fails, it is
	/// removed later by `deleteTrash`.
	public static Res<File> moveToTrash(File dir) {
		if (dir == null || !dir.exists())
			return Res.error("database folder does not exist: " + dir);
		var trash = new File(dir.getParentFile(),
			TRASH_PREFIX + dir.getName() + "-" + System.currentTimeMillis());
		try {
			Files.move(dir.toPath(), trash.toPath(),
				StandardCopyOption.ATOMIC_MOVE);
			return Res.ok(trash);
		} catch (Exception e) {
			return Res.error("failed to rename " + dir + " to " + trash, e);
		}
	}

	/// Returns `true` if the given folder is a database folder that was marked
	/// for deletion.
	public static boolean isTrash(File dir) {
		return dir != null && dir.getName().startsWith(TRASH_PREFIX);
	}

	/// Tries to delete the trash folders in the databases folder of the
	/// workspace. Errors are only logged, as the trash folders do not affect
	/// the application.
	public static void deleteTrash() {
		var dirs = Workspace.dbDir().listFiles();
		if (dirs == null)
			return;
		for (var dir : dirs) {
			if (!isTrash(dir))
				continue;
			try {
				Dirs.delete(dir);
			} catch (Exception e) {
				LoggerFactory.getLogger(DatabaseDir.class)
					.warn("failed to delete database trash folder {}", dir, e);
			}
		}
	}

	/// Get the general location for storing additional files for a database (in
	/// general this is database_name/_olca_)
	public static File getFileStorageLocation(IDatabase db) {
		return db.getFileStorageLocation() != null
				? db.getFileStorageLocation()
				: new File(getRootFolder(db.getName()), FILE_STORAGE);
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
