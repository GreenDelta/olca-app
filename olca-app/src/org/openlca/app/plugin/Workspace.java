package org.openlca.app.plugin;

import java.io.File;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.Platform;
import org.openlca.core.application.CommandArgument;

/**
 * The workspace configuration of openLCA. The workspace is located in the
 * folder "openLCA-data" in the user's home directory (system property
 * user.home).
 */
public class Workspace {

	private static File dir;

	/**
	 * Initializes the workspace of the application. Should be called when the
	 * application bundle starts.
	 */
	static File init() {
		try {
			Platform.getInstanceLocation().release();
			File dir = getDirFromCommandLine();
			if (dir == null)
				dir = getFromUserHome();
			URL workspaceUrl = new URL("file", null, dir.getAbsolutePath());
			Platform.getInstanceLocation().set(workspaceUrl, true);
			initMySQLData(dir);
			Workspace.dir = dir;
			return dir;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static File getFromUserHome() {
		String prop = System.getProperty("user.home");
		File userDir = new File(prop);
		File dir = new File(userDir, "openLCA-data");
		if (!dir.exists())
			dir.mkdirs();
		return dir;
	}

	private static File getDirFromCommandLine() {
		try {
			String path = CommandArgument.DATA_DIR.getValue();
			if (path == null)
				return null;
			File file = new File(path);
			if (file.canWrite() && file.isDirectory())
				return file;
			return null;
		} catch (Exception e) {
			// no logging here as the logger is not yet configured
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Get the workspace directory. Returns null if the workspace was not yet
	 * initialized.
	 */
	public static File getDir() {
		if (dir == null)
			init();
		return dir;
	}

	private static void initMySQLData(File workspaceDir) throws Exception {
		File dataDir = new File(workspaceDir, "data");
		if (dataDir.exists())
			return;
		File installDataDir = getInstallDataDir();
		if (installDataDir.exists()) {
			dataDir.mkdirs();
			FileUtils.copyDirectory(installDataDir, dataDir);
		}
	}

	private static File getInstallDataDir() throws Exception {
		String prop = System.getProperty("eclipse.home.location");
		URL url = new URL(prop);
		File eclipseHome = new File(url.getPath());
		File dataDir = new File(eclipseHome, "data");
		return dataDir;
	}

}
