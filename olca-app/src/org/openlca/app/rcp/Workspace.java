package org.openlca.app.rcp;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;

import org.eclipse.core.runtime.Platform;
import org.openlca.app.AppArg;
import org.openlca.app.Config;
import org.openlca.core.DataDir;
import org.openlca.core.library.LibraryDir;

/**
 * The workspace configuration of openLCA. The workspace is located in the
 * folder "openLCA-data" in the user's home directory (system property
 * user.home).
 */
public class Workspace {

	private static final DataDir dir = init();

	/**
	 * Get the workspace directory. Returns null if the workspace was not yet
	 * initialized.
	 */
	public static File root() {
		return dir.root();
	}

	public static LibraryDir getLibraryDir() {
		return dir.getLibraryDir();
	}

	/**
	 * Returns the folder where the databases are stored.
	 */
	public static File dbDir() {
		return dir.getDatabasesDir();
	}

	/**
	 * Initializes the workspace of the application. Should be called only once
	 * when the application bundle starts.
	 */
	private static DataDir init() {
		try {
			Platform.getInstanceLocation().release();
			File dir = getDirFromCommandLine();
			if (dir == null) {
				dir = Config.WORK_SPACE_IN_USER_DIR
					? getFromUserHome()
					: getFromInstallLocation();
			}
			URL workspaceUrl = new URL("file", null, dir.getAbsolutePath());
			Platform.getInstanceLocation().set(workspaceUrl, true);
			return DataDir.get(dir);
		} catch (Exception e) {
			// no logging here as the logger is not yet configured
			e.printStackTrace();
			return DataDir.get();
		}
	}

	private static File getFromUserHome() {
		String prop = System.getProperty("user.home");
		File userDir = new File(prop);
		return new File(userDir, Config.WORK_SPACE_FOLDER_NAME);
	}

	private static File getFromInstallLocation() throws Exception {
		URI uri = Platform.getInstallLocation().getURL().toURI();
		File installDir = new File(uri);
		return new File(installDir, Config.WORK_SPACE_FOLDER_NAME);
	}

	private static File getDirFromCommandLine() {
		try {
			var path = AppArg.DATA_DIR.getValue();
			if (path == null)
				return null;
			var dir = new File(path);
			if (!dir.exists()) {
				Files.createDirectories(dir.toPath());
			}
			if (dir.isDirectory() && dir.canWrite())
				return dir;
			// no logging here as the logger is not yet configured
			System.err.println("cannot switch workspace; " +
				"not a writeable folder: " + dir);
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
