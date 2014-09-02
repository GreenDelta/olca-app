package org.openlca.app.rcp;

import java.io.File;
import java.net.URL;

import org.eclipse.core.runtime.Platform;
import org.openlca.app.CommandArgument;
import org.openlca.app.Config;

/**
 * The workspace configuration of openLCA. The workspace is located in the
 * folder "openLCA-data" in the user's home directory (system property
 * user.home).
 */
public class Workspace {

	private static File dir;

	/**
	 * Get the workspace directory. Returns null if the workspace was not yet
	 * initialized.
	 */
	public static File getDir() {
		if (dir == null)
			init();
		return dir;
	}

	/**
	 * Initializes the workspace of the application. Should be called only once
	 * when the application bundle starts.
	 */
	static File init() {
		try {
			Platform.getInstanceLocation().release();
			File dir = getDirFromCommandLine();
			if (dir == null)
				dir = getFromUserHome();
			URL workspaceUrl = new URL("file", null, dir.getAbsolutePath());
			Platform.getInstanceLocation().set(workspaceUrl, true);
			Workspace.dir = dir;
			return dir;
		} catch (Exception e) {
			// no logging here as the logger is not yet configured
			e.printStackTrace();
			return null;
		}
	}

	private static File getFromUserHome() {
		String prop = System.getProperty("user.home");
		File userDir = new File(prop);
		File dir = new File(userDir, Config.WORK_SPACE_FOLDER_NAME);
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

}
